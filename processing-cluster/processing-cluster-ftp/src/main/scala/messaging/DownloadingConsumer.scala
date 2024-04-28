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
import ftp.FtpDownloader
import storage.FileManager
import types.OpaqueTypes.*
import upickle.default.read
import upickle.default.write

import java.util.concurrent.Executors

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
  * @param downloader
  *   the ftp downloader to use
  */
case class DownloadingConsumer(
    channel: Channel,
    routingKey: RoutingKey,
    failKey: RoutingKey,
    publishFunction: (Channel, String, RoutingKey) => Either[String, String],
    downloader: FtpDownloader
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
    val ftp_file_path = messageMap("ftp_file_path")

    FtpDownloader.fileFromPath(downloader, ftp_file_path) match
      case Left(error) =>
        val errorMap = messageMap + ("error" -> error) + ("status" -> "aborted")
        val errorString = write(errorMap)
        publishFunction(channel, errorString, failKey)
      case Right(file) =>
        FileManager.fileToLocal(file, ftp_file_path) match
          case Left(error) =>
            val errorMap =
              messageMap + ("error" -> error) + ("status" -> "aborted")
            val errorString = write(errorMap)
            publishFunction(channel, errorString, failKey)
          case Right(_) =>
            publishFunction(channel, message, routingKey)

    channel.basicAck(envelope.getDeliveryTag(), false)

  /** The startScheduledNoOp function starts a scheduled task to send a NoOp
    * command to the ftp server every minute.
    */
  def startScheduledNoOp(): Unit =
    executorService.scheduleAtFixedRate(
      () =>
        val noopResult = FtpDownloader.sendNoOp(downloader)
        println(s" [-] NoOp result: $noopResult")
      ,
      0,
      1,
      java.util.concurrent.TimeUnit.MINUTES
    )
