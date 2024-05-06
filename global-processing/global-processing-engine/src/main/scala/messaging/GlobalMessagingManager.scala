/** The GlobalMessagingManager object is responsible for configuring the global
  * processing engine. It configures the global processing engine messaging.
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

import java.io.UnsupportedEncodingException

private val Success = "Success"

private val GlobalProcessingExchange = ExchangeName(
  "global_processing_exchange"
)
private val GlobalProcessingExchangeType = ExchangeType.Topic
private val UserTasksQueue = QueueName("federated_user_tasks_queue")
private val GlobalProcessingQueue = QueueName(
  "federated_global_processing_queue"
)
private val GlobalProcessingRoutingKey = RoutingKey("global.task.processing")
private val GlobalResultsQueue = QueueName(
  "federated_global_results_queue"
)
private val UserResultsQueue = QueueName("federated_user_results_queue")
private val UserResultsRoutingKey = RoutingKey("user.task.results")


/** The GlobalMessagingManager object is responsible for configuring the local
  * and global clusters.
  */
object GlobalMessagingManager:

  /** The configureGlobalMessaging function configures the global processing
    * messaging.
    *
    * @param channel
    *   the channel to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def configureGlobalMessaging(
      channel: Channel
  ): Either[String, String] =
    val configurationRestult =
      for
        _globalProcessingExchangeDeclared <- MessagingUtil.channelWithExchange(
          channel,
          GlobalProcessingExchange,
          GlobalProcessingExchangeType
        )

        _userTasksQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          UserTasksQueue
        )

        _globalProcessingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          GlobalProcessingQueue
        )

        _globalProcessingQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            GlobalProcessingQueue,
            GlobalProcessingExchange,
            GlobalProcessingRoutingKey
          )

        _globalResultQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          GlobalResultsQueue
        )

        _userResultsQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          UserResultsQueue
        )

        _userResultsQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            UserResultsQueue,
            GlobalProcessingExchange,
            UserResultsRoutingKey
          )

        _channelPublisherConfirmsEnabled <- MessagingUtil
          .channelWithPublisherConfirms(channel)
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
        GlobalProcessingExchange,
        routingKey,
        messageBytes
      )

      result match
        case Left(error) =>
          println(error)
          Left(error)
        case Right(msg) =>
          println(s" [x] Sent '$message'")
          Right(msg)

    catch
      case e: UnsupportedEncodingException =>
        return Left(s"Error: ${e.getMessage}")

  /** The createTaskConsumer function creates a consumer for the global
    * prcessing engine.
    *
    * @param localChannel
    *   the local channel
    *
    * @return
    *   the consumer
    */
  def createTaskConsumer(
      localChannel: Channel
  ): DefaultConsumer =
    TaskConsumer(
      localChannel,
      GlobalProcessingRoutingKey,
      publishMessage
    )

  /** The createResultConsumer function creates a consumer for the global
    * processing engine.
    *
    * @param localChannel
    *   the local channel
    *
    * @return
    *   the consumer
    */
  def createResultConsumer(
      localChannel: Channel
  ): DefaultConsumer =
    ResultConsumer(
      localChannel,
      UserResultsRoutingKey,
      publishMessage
    )

  /** The consumeTaskQueue function consumes the task queue.
    * @param channel
    *   the channel to use
    * @param queue
    *   the queue to consume
    * @param autoAck
    *   whether to auto ack the messages
    * @param consumer
    *   the consumer to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def consumeTaskQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      UserTasksQueue,
      autoAck,
      consumer
    )

  /** The consumeResultQueue function consumes the result queue.
    *
    * @param channel
    *   the channel to use
    * @param autoAck
    *   whether to auto ack the messages
    * @param consumer
    *   the consumer to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def consumeResultQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      GlobalResultsQueue,
      autoAck,
      consumer
    )
