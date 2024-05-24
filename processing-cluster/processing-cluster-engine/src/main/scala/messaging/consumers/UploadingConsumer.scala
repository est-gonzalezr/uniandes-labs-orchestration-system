/** The UploadingConsumer class is a consumer for the file uploading queue. It
  * uploads a file to the ftp server and publishes a message to the next queue.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package messaging.consumers

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

private val UploadingStatus = "status_ftp_uploading"

/** The UploadingConsumer class is a consumer for the file uploading queue. It
  * uploads a file to the ftp server and publishes a message to the next queue.
  *
  * @param channel
  *   the channel to use
  * @param routingKey
  *   the routing key to use
  * @param publishFunction
  *   the function to publish a message
  * @param ftpWorker
  *   the ftp worker to use
  */
case class UploadingConsumer(
    channel: Channel,
    routingKey: RoutingKey,
    failKey: RoutingKey,
    publishFunction: (Channel, String, RoutingKey) => Either[String, String],
    ftpWorker: FtpWorker
) extends DefaultConsumer(channel):
  private val executorService: java.util.concurrent.ScheduledExecutorService =
    Executors.newScheduledThreadPool(1)

  /** The handleDelivery function handles the delivery of a message from the
    * queue. It tries to upload a file to the ftp server and depending on the
    * result it publishes a message to the next queue or to the fail queue.
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
    val ftpUploadingPath = messageMap("ftp_uploading_path")

    val operationResult: Either[String, Any] =
      FileManager.fileFromLocal(ftpUploadingPath) match
        case Left(error) =>
          handleError(error, messageMap, channel, failKey)
          Left(error)
        case Right(fileBytes) =>
          FtpWorker.fileToPath(
            ftpWorker,
            ftpUploadingPath,
            fileBytes
          ) match
            case Left(error) =>
              handleError(error, messageMap, channel, failKey)
              Left(error)
            case Right(_) => Right(messageMap)

    operationResult match
      case Left(error) => println(s"Error uploading file: $error")
      case Right(_)    => handleSuccess(messageMap, channel, routingKey)

    handleLocalDelete(ftpUploadingPath)

    channel.basicAck(envelope.getDeliveryTag, false)

  /** The startScheduledNoOp function starts a scheduled task to send a NoOp
    * command to the ftp server every minute.
    */
  def startScheduledNoOp(): Unit =
    executorService.scheduleAtFixedRate(
      () =>
        val noopResult = FtpWorker.sendNoOp(ftpWorker)
        noopResult match
          case Right(_) =>
            println(" [+] NoOp command sent successfully")
          case Left(error) =>
            println(
              s" [-] Error sending NoOp command: $error. Shutting down..."
            )
            sys.exit(1)
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
      messageMap + (UploadingStatus -> "failed") + ("error" -> error)
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
    val successMap = messageMap + (UploadingStatus -> "success")
    val successString = write(successMap)
    publishFunction(channel, successString, routingKey)

    /** The handleLocalDelete function deletes a file from the local filesystem.
      *
      * @param file_path
      *   the path of the file to delete
      */
  def handleLocalDelete(
      file_path: String
  ): Unit =
    FileManager.deleteFile(file_path) match
      case Left(error) =>
        println(s"Error deleting file from local filesystem: $error")
      case Right(_) =>
        println(s"File deleted from local filesystem: $file_path")
