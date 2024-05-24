/** The MessagingUtil object provides utility functions to interact with the
  * RabbitMQ broker.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package messaging

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DeliverCallback
import com.typesafe.config.ConfigException.IO
import configuration.ConfigurationUtil.configRabbitmqHost
import configuration.ConfigurationUtil.configRabbitmqPassword
import configuration.ConfigurationUtil.configRabbitmqPort
import configuration.ConfigurationUtil.configRabbitmqUsername
import configuration.ConfigurationUtil.envRabbitmqHost
import configuration.ConfigurationUtil.envRabbitmqPassword
import configuration.ConfigurationUtil.envRabbitmqPort
import configuration.ConfigurationUtil.envRabbitmqUsername
import types.ExchangeType
import types.OpaqueTypes.*

import java.io.IOException
import java.util.concurrent.TimeoutException

/** The MessagingUtil object provides utility functions to interact with the
  * RabbitMQ broker.
  */
object MessagingUtil:
  /** The connection function creates a connection to the RabbitMQ broker.
    *
    * @param host
    *   the RabbitMQ host
    * @param port
    *   the RabbitMQ port
    * @param username
    *   the RabbitMQ username
    * @param password
    *   the RabbitMQ password
    *
    * @return
    *   either the RabbitMQ connection or an error message
    */
  private def connection(
      host: String,
      port: Int,
      username: String,
      password: String
  ): Either[String, Connection] =
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    factory.setUsername(username)
    factory.setPassword(password)

    try
      val connection = factory.newConnection()
      Right(connection)
    catch
      case e: IOException =>
        Left("IOException: Cannot create connection")
      case e: TimeoutException =>
        Left("TimeoutException: Connection timeout")

  /** The envBrokerConnection function creates a connection to the local
    * RabbitMQ broker.
    *
    * @return
    *   either the local RabbitMQ connection or an error message
    */
  private def envBrokerConnection: Either[String, Connection] =
    for
      host <- envRabbitmqHost
      port <- envRabbitmqPort
      username <- envRabbitmqUsername
      password <- envRabbitmqPassword
      conn <- connection(host, port, username, password)
    yield conn

  /** The configBrokerConnection function creates a connection to the local
    * RabbitMQ broker.
    *
    * @return
    *   either the local RabbitMQ connection or an error message
    */
  private def configBrokerConnection: Either[String, Connection] =
    for
      host <- configRabbitmqHost
      port <- configRabbitmqPort
      username <- configRabbitmqUsername
      password <- configRabbitmqPassword
      conn <- connection(host, port, username, password)
    yield conn

  /** The connectionFromContext function creates a connection to the defined
    * RabbitMQ connection.
    *
    * @param dockerEnv
    *   a boolean indicating if the service is running in a docker environment
    *
    * @return
    *   either the RabbitMQ connection or an error message
    */
  def connectionFromContext(
      dockerEnv: Boolean = true
  ): Either[String, Connection] =
    if dockerEnv then envBrokerConnection
    else configBrokerConnection

  /** The channelDefinition function creates a channel to the defined RabbitMQ
    * connection.
    *
    * @param connection
    *   the RabbitMQ connection
    *
    * @return
    *   either the RabbitMQ channel or an error message
    */
  def channelDefinition(
      connection: Connection
  ): Either[String, Channel] =
    try
      val channel = connection.createChannel()
      Right(channel)
    catch case e: IOException => Left("IOException: Cannot create channel")

  /** The channelWithExchange function creates an exchange on the defined
    * RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param exchangeName
    *   the name of the exchange
    * @param exchangeType
    *   the type of the exchange
    * @param durable
    *   the durability of the exchange
    * @param autoDelete
    *   the auto-deletion of the exchange
    * @param internal
    *   the internal flag of the exchange
    *
    * @return
    *   the operation result of creating the exchange
    */
  def channelWithExchange(
      channel: Channel,
      exchangeName: ExchangeName,
      exchangeType: ExchangeType,
      durable: Boolean = false,
      autoDelete: Boolean = false,
      internal: Boolean = false
  ): Either[String, String] =
    try
      channel.exchangeDeclare(
        exchangeName.value,
        exchangeType.toString.toLowerCase(),
        durable,
        autoDelete,
        internal,
        null
      )

      Right(s"Exchange created: ${exchangeName.value}")
    catch
      case e: IOException =>
        Left(s"IOException: Cannot create exchange: ${exchangeName.value}")

  /** The channelWithQueue function creates a queue on the defined RabbitMQ
    * channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param queueName
    *   the name of the queue
    * @param durable
    *   the durability of the queue
    * @param exclusive
    *   the exclusivity of the queue
    * @param autoDelete
    *   the auto-deletion of the queue
    *
    * @return
    *   the operation result of creating the queue
    */
  def channelWithQueue(
      channel: Channel,
      queueName: QueueName,
      durable: Boolean = false,
      exclusive: Boolean = false,
      autoDelete: Boolean = false
  ): Either[String, String] =
    try
      channel.queueDeclare(
        queueName.value,
        durable,
        exclusive,
        autoDelete,
        null
      )

      Right(s"Queue created: ${queueName.value}")
    catch
      case e: IOException =>
        Left(s"IOException: Cannot create queue: ${queueName.value}")

  /** The bindedQueueWithExchange function binds a queue to an exchange on the
    * defined RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param queueName
    *   the name of the queue
    * @param exchangeName
    *   the name of the exchange
    * @param routingKey
    *   the routing key
    *
    * @return
    *   the operation result of binding the queue to the exchange
    */
  def bindedQueueWithExchange(
      channel: Channel,
      queueName: QueueName,
      exchangeName: ExchangeName,
      routingKey: RoutingKey
  ): Either[String, String] =
    try
      channel.queueBind(
        queueName.value,
        exchangeName.value,
        routingKey.value
      )

      Right(
        s"Queue: ${queueName.value} bound to exchange: ${exchangeName.value} with routing key: ${routingKey.value}"
      )
    catch
      case e: IOException =>
        Left(
          s"IOException: Cannot bind queue: ${queueName.value} bound to exchange: ${exchangeName.value} with routing key: ${routingKey.value}"
        )

  /** The bindedExchangeWithExchange function binds an exchange to another
    * exchange on the defined RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param sourceExchangeName
    *   the name of the source exchange
    * @param destinationExchangeName
    *   the name of the destination exchange
    * @param routingKey
    *   the routing key
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def bindedExchangeWithExchange(
      channel: Channel,
      destinationExchangeName: ExchangeName,
      sourceExchangeName: ExchangeName,
      routingKey: RoutingKey
  ): Either[String, String] =
    try
      channel.exchangeBind(
        destinationExchangeName.value,
        sourceExchangeName.value,
        routingKey.value
      )

      Right(
        s"Exchange: ${destinationExchangeName.value} bound to exchange: ${sourceExchangeName.value} with routing key: ${routingKey.value}"
      )
    catch
      case e: IOException =>
        Left(
          s"IOException: Cannot bind exchange: ${destinationExchangeName.value} bound to exchange: ${sourceExchangeName.value} with routing key: ${routingKey.value}"
        )

  /** The channelWithQos function sets the quality of service on the defined
    * RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param prefetchSize
    *   the prefetch size
    * @param prefetchCount
    *   the prefetch count
    * @param global
    *   the global flag
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  def channelWithQos(
      channel: Channel,
      prefetchSize: Int = 0,
      prefetchCount: Int = 0,
      global: Boolean = false
  ): Either[String, String] =
    try
      channel.basicQos(prefetchSize, prefetchCount, global)
      Right(s"Quality of service set")
    catch
      case e: IOException => Left("IOException: Cannot set quality of service")

  /** The channelWithPublisherConfirms function enables publisher confirms on
    * the defined RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */

  def channelWithPublisherConfirms(channel: Channel): Either[String, String] =
    try
      channel.confirmSelect()
      Right(s"Publisher confirms enabled")
    catch
      case e: IOException =>
        Left("IOException: Cannot enable publisher confirms")

  /** The sendMessage function sends a message to an exchange on the defined
    * RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param exchangeName
    *   the name of the exchange
    * @param routingKey
    *   the routing key
    * @param messageBytes
    *   the message bytes
    * @param properties
    *   the message properties
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  protected[messaging] def publishMessage(
      channel: Channel,
      exchangeName: ExchangeName,
      routingKey: RoutingKey,
      messageBytes: Array[Byte],
      properties: BasicProperties = null
  ): Either[String, String] =
    try
      channel.basicPublish(
        exchangeName.value,
        routingKey.value,
        properties,
        messageBytes
      )

      Right(
        s"Message sent to exchange: ${exchangeName.value} with routing key: ${routingKey.value}"
      )
    catch
      case e: IOException =>
        Left(
          s"IOException: Cannot send message to exchange: ${exchangeName.value} with routing key: ${routingKey.value}"
        )

  /** The consumeMessages function consumes messages from a queue on the defined
    * RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    * @param queueName
    *   the name of the queue
    * @param autoAck
    *   the auto-acknowledgement of the message
    * @param callback
    *   the message callback
    *
    * @return
    *   either a string with the error message or a string with the success
    *   message
    */
  protected[messaging] def consumeMessages(
      channel: Channel,
      queueName: QueueName,
      autoAck: Boolean = true,
      consumer: Consumer
  ): Either[String, String] =
    try
      channel.basicConsume(
        queueName.value,
        autoAck,
        consumer
      )

      Right(s"Consuming messages from queue: ${queueName.value}")
    catch
      case e: IOException =>
        Left(
          s"IOException: Cannot consume messages from queue: ${queueName.value}"
        )

  /** The closeChannel function closes the RabbitMQ channel.
    *
    * @param channel
    *   the RabbitMQ channel
    *
    * @return
    *   the operation result of closing the channel
    */
  protected[messaging] def closeChannel(
      channel: Channel
  ): Either[String, String] =
    try
      channel.close()
      Right("Channel closed")
    catch
      case e: IOException => Left("IOException: Cannot close channel")
      case e: TimeoutException =>
        Left("TimeoutException: Channel close timeout")

  /** The closeConnection function closes the RabbitMQ connection.
    *
    * @param connection
    *   the RabbitMQ connection
    *
    * @return
    *   the operation result of closing the connection
    */
  protected[messaging] def closeConnection(
      connection: Connection
  ): Either[String, String] =
    try
      connection.close()
      Right("Connection closed")
    catch case e: IOException => Left("IOException: Cannot close connection")
