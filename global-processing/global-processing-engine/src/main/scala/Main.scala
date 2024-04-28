/** The Main object is the entry point of the application. It configures the
  * global processing engine.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

import com.rabbitmq.client.Channel
import messaging.GlobalMessagingManager
import messaging.MessagingUtil

/** The main function configures the global processing engine.
  *
  * @param args
  *   the arguments passed to the program
  */
@main def main(args: String*): Unit =
  val executeLocal = args.contains("--local")
  val configurationResult = executeConfigurations(!executeLocal)

  configurationResult match
    case Right(channel) =>
      startTaskConsumer(channel)
      startResultConsumer(channel)
    case Left(error) =>
      println(s"Error: $error")

  while true do Thread.sleep(10000)

/** The executeConfigurations function configures global messaging.
  *
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  */
def executeConfigurations(dockerEnv: Boolean = true): Either[String, Channel] =
  println("Configuring global messaging...")

  val channel: Either[String, Channel] =
    for
      conn <- MessagingUtil.connectionFromContext(dockerEnv)
      channel <- MessagingUtil.channelDefinition(conn)
      _ <- GlobalMessagingManager.configureGlobalMessaging(
        channel
      )
    yield channel

  channel match
    case Left(error) =>
      println(s"Error configuring the global engine: $error")
      Left(error)
    case Right(channel) =>
      println("Global engine configured successfully")
      Right(channel)

/** The startTaskConsumer function starts the task consumers that consume the
  * user tasks queue and publish the messages to the global processing engine.
  *
  * @param localChannel
  *   the local channel
  */
def startTaskConsumer(localChannel: Channel): Unit =
  println("Starting task consumer...")

  val consumer = GlobalMessagingManager.createTaskConsumer(localChannel)

  GlobalMessagingManager.consumeTaskQueue(localChannel, false, consumer) match
    case Left(error) => println(s"Error starting a task consumer: $error")
    case Right(msg)  => println(s"Task consumer started successfully: $msg")

/** The startResultConsumer function starts the result consumers that consume
  * the global results queue and publish the messages to the user results queue
  * for further processing.
  *
  * @param localChannel
  *   the local channel
  */
def startResultConsumer(localChannel: Channel): Unit =
  println("Starting result consumer...")

  val consumer = GlobalMessagingManager.createResultConsumer(localChannel)

  GlobalMessagingManager.consumeResultQueue(localChannel, false, consumer) match
    case Left(error) => println(s"Error starting a result consumer: $error")
    case Right(msg)  => println(s"Result consumer started successfully: $msg")
