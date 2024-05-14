/** The TaskProcessingManager object is responsible for configuring the task
  * processing portion of the cluster messaging.
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
import types.ExchangeType
import types.OpaqueTypes.*
import types.TaskType

import java.io.UnsupportedEncodingException

private val Success = "Success"

private val LocalProcessingExchange = ExchangeName("local_processing_exchange")
private val LocalProcessingExchangeType = ExchangeType.Topic
private val TaskProcessingQueue = QueueName("local_task_processing_queue")
private val GlobalResultsRoutingKey = RoutingKey("global.task.results")
private val TaskUploadingRoutingKey = RoutingKey("local.task.uploading")

/** The TaskProcessingManager object is responsible for configuring the task
  * processing portion of the cluster messaging.
  */
object TaskProcessingManager:
  /** The configureTaskProcessingMessaging function configures the task
    * processing messaging.
    *
    * @param channel
    *   the channel to use
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def configureTaskProcessingMessaging(
      channel: Channel
  ): Either[String, String] =
    val configurationRestult =
      for
        _localProcessingExchangeDeclared <- MessagingUtil.channelWithExchange(
          channel,
          LocalProcessingExchange,
          LocalProcessingExchangeType
        )

        _taskProcessingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          TaskProcessingQueue
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

  /** The createProcessingConsumer function creates a consumer for the task
    * processing queue.
    *
    * @param channel
    *   the channel to use for the consumer creation
    * @param taskType
    *   the type of task to process
    *
    * @return
    *   the consumer
    */
  def createProcessingConsumer(
      channel: Channel,
      taskType: TaskType
  ): DefaultConsumer =
    ProcessingConsumer(
      channel,
      TaskUploadingRoutingKey,
      GlobalResultsRoutingKey,
      publishMessage,
      taskType
    )

  /** The consumeTaskProcessingQueue consumes the task processing queue.
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
  def consumeTaskProcessingQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      TaskProcessingQueue,
      autoAck,
      consumer
    )
