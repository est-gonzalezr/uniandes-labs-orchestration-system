/** The DownloadingConsumer class is a consumer for the file downloading queue.
  * It downloads a file from the ftp server, saves it to the local filesystem
  * and publishes a message to the next queue.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package messaging

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Envelope
import ftp.FtpWorker
import storage.FileManager
import types.OpaqueTypes.*
import upickle.default.read
import upickle.default.write

import java.util.concurrent.Executors

private val DownloadingStatus = "status_ftp_downloading"

/** The DownloadingConsumer class is a consumer for the file downloading queue.
  * It downloads a file from the ftp server, saves it to the local filesystem
  * and publishes a message to the next queue. If a file can be downloaded then
  * it publishes a message to the next queue, if not it publishes a message to
  * the fail queue.
  *
  * @param channel
  *   the channel to use
  * @param routingKey
  *   the routing key to use
  * @param failKey
  *   the fail routing key to use
  * @param publishFunction
  *   the function to publish a message
  * @param ftpWorker
  *   the ftp worker to use
  */
case class DownloadingConsumer(
    channel: Channel,
    routingKey: RoutingKey,
    failKey: RoutingKey,
    publishFunction: (Channel, String, RoutingKey) => Either[String, String],
    ftpWorker: FtpWorker
) extends DefaultConsumer(channel):
  private val executorService: java.util.concurrent.ScheduledExecutorService =
    Executors.newScheduledThreadPool(1)

  /** The handleDelivery function handles the delivery of a message from the
    * queue. It tries to download a file from the ftp server and depending on
    * the result it publishes a message to the next queue or to the fail queue.
    *
    * @param consumerTag
    * @param envelope
    * @param properties
    * @param body
    */
  override def handleDelivery(
      consumerTag: String,
      envelope: Envelope,
      properties: AMQP.BasicProperties,
      body: Array[Byte]
  ): Unit =
    val message = body.map(_.toChar).mkString
    println(s" [x] Received $message")

    // point of failure if any key or value is not a string
    val messageMap = read[Map[String, String]](body)
    val ftp_downloading_path = messageMap("ftp_downloading_path")

    val operationResult =
      FtpWorker.fileFromPath(ftpWorker, ftp_downloading_path) match
        case Left(error) =>
          handleError(error, messageMap, channel, failKey)
          Left(error)
        case Right(fileBytes) =>
          FileManager.fileToLocal(fileBytes, ftp_downloading_path) match
            case Left(error) =>
              handleError(error, messageMap, channel, failKey)
              Left(error)
            case Right(_) =>
              Right(messageMap + ("local_file_path" -> ftp_downloading_path))

    operationResult match
      case Left(error) => println(s"Error downloading file: $error")
      case Right(updatedMessageMap) =>
        handleSuccess(updatedMessageMap, channel, routingKey)

    handleFtpDelete(ftp_downloading_path)

    channel.basicAck(envelope.getDeliveryTag(), false)

  /** The startScheduledNoOp function starts a scheduled task to send a NoOp
    * command to the ftp server every minute.
    */
  def startScheduledNoOp(): Unit =
    executorService.scheduleAtFixedRate(
      () =>
        val noopResult = FtpWorker.sendNoOp(ftpWorker)
        println(s" [-] Downloading Consumer NoOp result: $noopResult")
      ,
      0,
      1,
      java.util.concurrent.TimeUnit.MINUTES
    )

  /** The handleError function handles an error by publishing a message with the
    * fail routing key.
    *
    * @param error
    *   the error message
    * @param messageMap
    *   the message map
    * @param channel
    *   the channel to use
    * @param failKey
    *   the fail routing key to use
    */
  def handleError(
      error: String,
      messageMap: Map[String, String],
      channel: Channel,
      failKey: RoutingKey
  ): Unit =
    val errorMap =
      messageMap + (DownloadingStatus -> "failed") + ("error" -> error)
    val errorString = write(errorMap)
    publishFunction(channel, errorString, failKey)

  /** The handleSuccess function handles a success by publishing a message with
    * the success routing key.
    *
    * @param messageMap
    *   the message map
    * @param channel
    *   the channel to use
    * @param routingKey
    *   the routing key to use
    */
  def handleSuccess(
      messageMap: Map[String, String],
      channel: Channel,
      routingKey: RoutingKey
  ): Unit =
    val successMap = messageMap + (DownloadingStatus -> "success")
    val successString = write(successMap)
    publishFunction(channel, successString, routingKey)

    /** The handleFtpDelete function deletes a file from the ftp server.
      *
      * @param ftp_file_path
      *   the path of the file to delete
      */
  def handleFtpDelete(
      ftp_file_path: String
  ): Unit =
    FtpWorker.deleteFromPath(ftpWorker, ftp_file_path) match
      case Left(error) =>
        println(s"Error deleting file from ftp server: $error")
      case Right(_) =>
        println(s"File deleted from ftp server : $ftp_file_path")
