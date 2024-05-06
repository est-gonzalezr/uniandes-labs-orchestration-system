/** The ClusterMessagingManager object is responsible for configuring the local
  * and global clusters. It also provides the functionality to publish messages
  * and create consumers for the cluster. Once a message enters the local
  * cluster, it is processed and then sent to the global processing cluster.
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

private val LocalProcessingExchange = ExchangeName("local_processing_exchange")
private val LocalProcessingExchangeType = ExchangeType.Topic
private val TaskDownloadingQueue = QueueName("local_task_downloading_queue")
private val TaskDownloadingRoutingKey = RoutingKey(
  "local.task.downloading"
)
private val TaskUploadingQueue = QueueName("local_task_uploading_queue")
private val TaskUploadingRoutingKey = RoutingKey("local.task.uploading")
private val TaskProcessingQueue = QueueName("local_task_processing_queue")
private val TaskProcessingRoutingKey = RoutingKey("local.task.processing")
private val GlobalProcessingExchangeType = ExchangeType.Topic
private val GlobalProcessingQueue = QueueName(
  "federated_global_processing_queue"
)
private val GlobalResultsQueue = QueueName("federated_global_results_queue")
private val GlobalResultsRoutingKey = RoutingKey("global.task.results")

/** The ClusterMessagingManager object is responsible for configuring the local
  * and global clusters.
  */
object ClusterMessagingManager:
  /** The configureLocalClusterMessaging function configures the local cluster
    * messaging.
    *
    * @param channel
    *   the channel to use
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def configureLocalClusterMessaging(
      channel: Channel
  ): Either[String, String] =
    val configurationRestult =
      for

        _localProcessingExchangeDeclared <- MessagingUtil.channelWithExchange(
          channel,
          LocalProcessingExchange,
          LocalProcessingExchangeType
        )

        _globalProcessingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          GlobalProcessingQueue
        )

        _globalResultsQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          GlobalResultsQueue
        )

        _globalResultsQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            GlobalResultsQueue,
            LocalProcessingExchange,
            GlobalResultsRoutingKey
          )

        _taskDownloadingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          TaskDownloadingQueue
        )

        _taskDownloadingQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            TaskDownloadingQueue,
            LocalProcessingExchange,
            TaskDownloadingRoutingKey
          )

        _taskUploadingQueueDeclared <- MessagingUtil.channelWithQueue(
          channel,
          TaskUploadingQueue
        )

        _taskUploadingQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            TaskUploadingQueue,
            LocalProcessingExchange,
            TaskUploadingRoutingKey
          )

        _taskProcessingQueueDeclared <- MessagingUtil
          .channelWithQueue(
            channel,
            TaskProcessingQueue
          )

        _taskProcessingQueueBindedToExchange <- MessagingUtil
          .bindedQueueWithExchange(
            channel,
            TaskProcessingQueue,
            LocalProcessingExchange,
            TaskProcessingRoutingKey
          )

        _channelQosDefined <- MessagingUtil.channelWithQos(
          channel,
          prefetchCount = 1
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
        LocalProcessingExchange,
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

  /** The createClusterConsumer function creates a consumer for the local
    * processing cluster.
    *
    * @param localChannel
    *   the local channel
    *
    * @return
    *   the consumer
    */
  def createClusterConsumer(
      localChannel: Channel
  ): DefaultConsumer =
    GlobalConsumer(
      localChannel,
      TaskDownloadingRoutingKey,
      publishMessage
    )

  /** The consumeGlobalQueue function consumes the global queue.
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
  def consumeGlobalQueue(
      channel: Channel,
      autoAck: Boolean,
      consumer: Consumer
  ): Either[String, String] =
    MessagingUtil.consumeMessages(
      channel,
      GlobalProcessingQueue,
      autoAck,
      consumer
    )
