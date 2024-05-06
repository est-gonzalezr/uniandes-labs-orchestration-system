/** The UploadingConsumer class is a consumer for the file uploading queue. It
  * uploads a file to the ftp server and publishes a message to the next queue.
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

    val messageMap = read[Map[String, String]](body)
    val ftp_file_path = messageMap("ftp_file_path")
    val ftp_file_destination = messageMap("ftp_file_destination")

    FileManager.fileFromLocal(ftp_file_path) match
      case Left(error) =>
        val errorMap = messageMap + ("error" -> error) + ("status" -> "aborted")
        val errorString = write(errorMap)
        publishFunction(channel, errorString, failKey)
      case Right(file) =>
        FtpWorker.fileToPath(
          ftpWorker,
          ftp_file_destination,
          file
        ) match
          case Right(_) =>
            publishFunction(channel, write(messageMap), routingKey)
          case Left(error) =>
            val errorMap =
              messageMap + ("error" -> error) + ("status" -> "aborted")
            val errorString = write(errorMap)
            publishFunction(channel, errorString, failKey)

    channel.basicAck(envelope.getDeliveryTag, false)

  def startScheduledNoOp(): Unit =
    executorService.scheduleAtFixedRate(
      () =>
        val noopResult = FtpWorker.sendNoOp(ftpWorker)
        println(s" [-] Uploading Consumer NoOp result: $noopResult")
      ,
      0,
      1,
      java.util.concurrent.TimeUnit.MINUTES
    )
