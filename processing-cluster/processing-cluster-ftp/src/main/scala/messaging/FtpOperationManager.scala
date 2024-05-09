/** The FtpOperationManager object is responsible for configuring the ftp
  * portion of the cluster messaging.
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
import types.ExchangeType
import types.OpaqueTypes.*

import java.io.UnsupportedEncodingException

private val Success = "Success"

private val LocalProcessingExchange = ExchangeName("local_processing_exchange")
private val LocalProcessingExchangeType = ExchangeType.Topic
private val TaskDownloadingQueue = QueueName("local_task_downloading_queue")
private val TaskUploadingQueue = QueueName("local_task_uploading_queue")
private val TaskProcessingRoutingKey = RoutingKey("local.task.processing")
private val GlobalResultsRoutingKey = RoutingKey("global.task.results")

/** The FtpOperationManager object is responsible for configuring the ftp
  * portion of the cluster messaging.
  */
object FtpOperationManager:
  /** The configureFileDownloaderMessaging function configures the local ftp
    * messaging.
    *
    * @param channel
    *   the channel to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def configureFileDownloadingMessaging(
      channel: Channel
  ): Either[String, String] =
    val configurationRestult =
      for
        _localProcessingExchangeDeclared <- MessagingUtil.channelWithExchange(
          channel,
          LocalProcessingExchange,
          LocalProcessingExchangeType
        )

        _taskDownloadingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          TaskDownloadingQueue
        )

        _taskUploadingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          TaskUploadingQueue
        )

        _channelQosDefined <- MessagingUtil.channelWithQos(
          channel,
          prefetchCount = 1
        )

        _channelPublisherConfirmsEnabled <- MessagingUtil
          .channelWithPublisherConfirms(
            channel
          )
      yield Success

    configurationRestult

  /** The publishMessage function sends a message through the specified channel
    * with a routing key.
    *
    * @param channel
    *   the channel to use
    * @param message
    *   the message to send
    * @param routingKey
    *   the routing key to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def publishMessage(
      channel: Channel,
      message: String,
      routingKey: RoutingKey
  ): Either[String, String] =
    try
      val messageBytes = message.getBytes("UTF-8")
      val result = MessagingUtil.publishMessage(
        channel,
        LocalProcessingExchange,
        routingKey,
        messageBytes
      )

      result match
        case Left(error) =>
          println(s"Error: $error")
          Left(error)
        case Right(msg) =>
          println(s" [x] Sent $message")
          Right(msg)

    catch
      case e: UnsupportedEncodingException =>
        return Left(s"Error: ${e.getMessage}")

  /** The createDownloadingConsumer function creates a consumer for the task
    * downloading queue.
    *
    * @param channel
    *   the channel to use for the consumer creation
    * @param dockerEnv
    *   a boolean indicating if the service is running in a docker environment
    *
    * @return
    *   either a string with the error message or the consumer
    */
  def createDownloadingConsumer(
      channel: Channel,
      dockerEnv: Boolean = true
  ): Either[String, DefaultConsumer] =
    val consumer =
      for downloader <- FtpWorker.apply(dockerEnv)
      yield DownloadingConsumer(
        channel,
        TaskProcessingRoutingKey,
        GlobalResultsRoutingKey,
        publishMessage,
        downloader
      )

    consumer match
      case Left(error) => Left(error)
      case Right(consumer) =>
        consumer.startScheduledNoOp()
        Right(consumer)

  /** The consumeTaskDownloadingQueue consumes the task downloading queue.
    *
    * @param channel
    *   the channel to use for the queue consumption
    * @param autoAck
    *   a boolean indicating whether the queue should auto-acknowledge messages
    * @param consumer
    *   the consumer to use for the queue consumption
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def consumeTaskDownloadingQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      TaskDownloadingQueue,
      autoAck,
      consumer
    )

  /** The createUploadingConsumer function creates a consumer for the task
    * uploading queue.
    *
    * @param channel
    *   the channel to use for the consumer creation
    * @param dockerEnv
    *   a boolean indicating if the service is running in a docker environment
    *
    * @return
    *   either a string with the error message or the consumer
    */
  def createUploadingConsumer(
      channel: Channel,
      dockerEnv: Boolean = true
  ): Either[String, DefaultConsumer] =
    val consumer =
      for uploader <- FtpWorker.apply(dockerEnv)
      yield UploadingConsumer(
        channel,
        GlobalResultsRoutingKey,
        GlobalResultsRoutingKey,
        publishMessage,
        uploader
      )

    consumer match
      case Left(error) => Left(error)
      case Right(consumer) =>
        consumer.startScheduledNoOp()
        Right(consumer)

  /** The consumeTaskUploadingQueue consumes the task uploading queue.
    *
    * @param channel
    *   the channel to use for the queue consumption
    * @param autoAck
    *   a boolean indicating whether the queue should auto-acknowledge messages
    * @param consumer
    *   the consumer to use for the queue consumption
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def consumeTaskUploadingQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      TaskUploadingQueue,
      autoAck,
      consumer
    )
