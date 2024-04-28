/** The Main object is the entry point of the ftp downloading service. It
  * configures the ftp downloading consumers and starts them.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

import com.rabbitmq.client.Channel
import configuration.ConfigurationUtil.configFtpConsumerQuantity
import configuration.ConfigurationUtil.envFtpConsumerQuantity
import messaging.FileDownloadingManager
import messaging.MessagingUtil

val DefaultFtpConsumerQuantity = 2

/** The main function is responsible for starting the ftp downloading service.
  *
  * @param args
  *   the arguments passed to the program
  */
@main def main(args: String*): Unit =
  val executeLocal = args.contains("--local")
  val channel = executeConfigurations(!executeLocal)
  val consumerQuantity =
    (if executeLocal then configFtpConsumerQuantity
     else envFtpConsumerQuantity).toOption
      .map(_.toInt)
      .getOrElse(DefaultFtpConsumerQuantity)

  channel match
    case Left(error) => ()
    case Right(channel) =>
      startFtpDownloadingConsumers(channel, consumerQuantity, !executeLocal)

  while true do Thread.sleep(10000)

/** The executeConfigurations function is responsible for executing the
  * configurations for the ftp downloading service.
  *
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  *
  * @return
  *   either a string with the error message or a channel
  */
def executeConfigurations(dockerEnv: Boolean = true): Either[String, Channel] =
  println("Configuring local ftp messaging...")
  val channelWithConfig =
    for
      conn <- MessagingUtil.connectionFromContext(dockerEnv)
      channel <- MessagingUtil.channelDefinition(conn)
      _ <- FileDownloadingManager
        .configureFileDownloadingMessaging(channel)
    yield channel

  channelWithConfig match
    case Left(error) =>
      println(s"Error: $error")
      Left(error)
    case Right(channel) =>
      println("Local ftp messaging configured successfully")
      Right(channel)

/** The startFtpDownloadingConsumers function is responsible for starting the
  * ftp downloading consumers.
  *
  * @param channel
  *   the channel to use for the ftp downloading consumers
  * @param quantity
  *   the quantity of consumers to start
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  *
  * @return
  *   a unit indicating the result of the function
  */
def startFtpDownloadingConsumers(
    channel: Channel,
    quantity: Int,
    dockerEnv: Boolean = true
): Unit =
  println(s"Starting $quantity ftp downloading consumers...")

  for _ <- 1 to quantity do
    val consumer =
      FileDownloadingManager.createDownloadingConsumer(
        channel,
        dockerEnv
      )

    consumer match
      case Left(error) => println(error)
      case Right(consumer) =>
        FileDownloadingManager.consumeTaskDownloadingQueue(
          channel,
          false,
          consumer
        ) match
          case Left(error) => println(s"Error starting a FTP consumer: $error")
          case Right(msg) => println(s"FTP consumer started successfully: $msg")
