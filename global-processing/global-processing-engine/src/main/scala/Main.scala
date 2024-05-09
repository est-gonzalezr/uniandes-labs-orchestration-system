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
    case Left(error) =>
      println(s"Error: $error")
    case Right(channel) =>
      startTaskConsumer(channel)
      startResultConsumer(channel)

      (startResultConsumer(channel), startTaskConsumer(channel)) match
        case (Left(_), Left(_)) =>
          println(
            "Neither the task consumer nor the result consumer started successfully: Shutting down..."
          )
        case (Left(_), Right(_)) =>
          println(
            "The task consumer started successfully, but the result consumer did not: Shutting down..."
          )
        case (Right(_), Left(_)) =>
          println(
            "The result consumer started successfully, but the task consumer did not: Shutting down..."
          )
        case (Right(_), Right(_)) =>
          println(
            "Both the task consumer and the result consumer started successfully: Running..."
          )
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
  *
  * @return
  *   either a string with an error message or a string indicating success
  */
def startTaskConsumer(localChannel: Channel): Either[String, String] =
  println("Starting task consumer...")

  val consumer = GlobalMessagingManager.createTaskConsumer(localChannel)

  GlobalMessagingManager.consumeTaskQueue(localChannel, false, consumer) match
    case Left(error) =>
      println(s"Error starting the task consumer: $error")
      Left(error)
    case Right(msg) =>
      println(s"Task consumer started successfully: $msg")
      Right(msg)

/** The startResultConsumer function starts the result consumers that consume
  * the global results queue and publish the messages to the user results queue
  * for further processing.
  *
  * @param localChannel
  *   the local channel
  *
  * @return
  *   either a string indicating an error or a string indicating success
  */
def startResultConsumer(localChannel: Channel): Either[String, String] =
  println("Starting result consumer...")

  val consumer = GlobalMessagingManager.createResultConsumer(localChannel)

  GlobalMessagingManager.consumeResultQueue(localChannel, false, consumer) match
    case Left(error) =>
      println(s"Error starting the result consumer: $error")
      Left(error)
    case Right(msg) =>
      println(s"Result consumer started successfully: $msg")
      Right(msg)
