/** The ResultConsumer send the results from processing clusters to a queue to
  * be sent to the useres. It is responsible for getting the messages from the
  * global processing engine to the users with the result of their execution.
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
import types.OpaqueTypes.*

/** The ResultConsumer class is a consumer for the global procesing engine. It
  * connects to the remote queue and consumes the messages from the remote
  * queue. Then, it publishes the message to the local channel so that the local
  * global processing engine can process it.
  *
  * @param localChannel
  *   the local channel
  * @param routingKey
  *   the routing key
  * @param publishFunction
  *   the function to execute
  *
  * @return
  *   a new ResultConsumer
  */
case class ResultConsumer(
    localChannel: Channel,
    routingKey: RoutingKey,
    publishFunction: (Channel, String, RoutingKey) => Either[String, String]
) extends DefaultConsumer(localChannel):
  /** The handleDelivery function handles the delivery of the message by the
    * consumer. This function consumes the message from the remote queue and
    * publishes it to the local exchange.
    *
    * @param consumerTag
    *   the consumer tag
    * @param envelope
    *   the envelope
    * @param properties
    *   the properties
    * @param body
    *   the body
    */
  override def handleDelivery(
      consumerTag: String,
      envelope: Envelope,
      properties: AMQP.BasicProperties,
      body: Array[Byte]
  ): Unit =
    val message = body.map(_.toChar).mkString
    println(s" [x] Received $message")
    publishFunction(localChannel, message, routingKey)
    localChannel.basicAck(envelope.getDeliveryTag(), false)
