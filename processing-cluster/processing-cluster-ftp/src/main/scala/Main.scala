/** The Main object is the entry point of the ftp downloading service. It
  * configures the ftp downloading consumers and starts them.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

import com.rabbitmq.client.Channel
import configuration.ConfigurationUtil.configFtpDownloadingConsumerQuantity
import configuration.ConfigurationUtil.configFtpUploadingConsumerQuantity
import configuration.ConfigurationUtil.envFtpDownloadingConsumerQuantity
import configuration.ConfigurationUtil.envFtpUploadingConsumerQuantity

import messaging.FtpOperationManager
import messaging.MessagingUtil

val DefaultFtpDownloadingConsumerQuantity = 2
val DefaultFtpUploadingConsumerQuantity = 2

/** The main function is responsible for starting the ftp downloading and
  * uploadng service.
  *
  * @param args
  *   the arguments passed to the program
  */
@main def main(args: String*): Unit =
  val executeLocal = args.contains("--local")
  val channel = executeConfigurations(!executeLocal)
  val downloadingConsumerQuantity =
    (if executeLocal then configFtpDownloadingConsumerQuantity
     else envFtpDownloadingConsumerQuantity).toOption
      .map(_.toInt)
      .filter(_ > 0)
      .getOrElse(DefaultFtpDownloadingConsumerQuantity)

  val uploadingConsumerQuantity =
    (if executeLocal then configFtpUploadingConsumerQuantity
     else envFtpUploadingConsumerQuantity).toOption
      .map(_.toInt)
      .filter(_ > 0)
      .getOrElse(DefaultFtpUploadingConsumerQuantity)

  channel match
    case Left(error) => ()
    case Right(channel) =>
      val allDownloadersStartupState = startFtpDownloadingConsumers(
        channel,
        downloadingConsumerQuantity,
        !executeLocal
      ).foldLeft(
        Right(
          "All downloading consumers started successfully: Running..."
        ): Either[
          String,
          String
        ]
      )((acc, donwloaderState) =>
        donwloaderState match
          case Left(_) =>
            Left(
              "Not all downloading consumers started successfully: Shutting down..."
            )
          case Right(_) =>
            acc
      )

      val allUploadersStartupState = startFtpUploadingConsumers(
        channel,
        uploadingConsumerQuantity,
        !executeLocal
      ).foldLeft(
        Right(
          "All uploading consumers started successfully: Running..."
        ): Either[
          String,
          String
        ]
      )((acc, uploaderState) =>
        uploaderState match
          case Left(_) =>
            Left(
              "Not all uploading consumers started successfully: Shutting down..."
            )
          case Right(_) =>
            acc
      )

      (allDownloadersStartupState, allUploadersStartupState) match
        case (Left(_), Left(_)) =>
          println(
            "Neither the downloading nor the uploading consumers started successfully: Shutting down..."
          )
        case (Left(error), Right(_)) =>
          println(
            "The uploading consumers started successfully, but the downloading consumers did not: Shutting down..."
          )
        case (Right(_), Left(error)) =>
          println(
            "The downloading consumers started successfully, but the uploading consumers did not: Shutting down..."
          )
        case _ =>
          println(
            "All donwloading and uploading consumers started successfully: Running..."
          )
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
      _ <- FtpOperationManager
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
  *   a sequence of either a string with the error message or a string with the
  *   success message
  */
def startFtpDownloadingConsumers(
    channel: Channel,
    quantity: Int,
    dockerEnv: Boolean = true
): IndexedSeq[Either[String, String]] =
  println(s"Starting $quantity ftp downloading consumers...")

  for _ <- 1 to quantity
  yield FtpOperationManager.createDownloadingConsumer(
    channel,
    dockerEnv
  ) match
    case Left(error) =>
      println(error)
      Left(error)
    case Right(consumer) =>
      FtpOperationManager.consumeTaskDownloadingQueue(
        channel,
        false,
        consumer
      ) match
        case Left(error) =>
          println(s"Error starting a FTP downloading consumer: $error")
          Left(error)
        case Right(msg) =>
          println(s"FTP downloading consumer started successfully: $msg")
          Right(msg)

/** The startFtpUploadingConsumers function is responsible for starting the ftp
  * uploading consumers.
  *
  * @param channel
  *   the channel to use for the ftp uploading consumers
  * @param quantity
  *   the quantity of consumers to start
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  *
  * @return
  *   a sequence of either a string with the error message or a string with the
  *   success message
  */
def startFtpUploadingConsumers(
    channel: Channel,
    quantity: Int,
    dockerEnv: Boolean = true
): IndexedSeq[Either[String, String]] =
  println(s"Starting $quantity ftp uploading consumers...")

  for _ <- 1 to quantity
  yield FtpOperationManager.createUploadingConsumer(
    channel,
    dockerEnv
  ) match
    case Left(error) =>
      println(error)
      Left(error)
    case Right(consumer) =>
      FtpOperationManager.consumeTaskUploadingQueue(
        channel,
        false,
        consumer
      ) match
        case Left(error) =>
          println(s"Error starting a FTP uploading consumer: $error")
          Left(error)
        case Right(msg) =>
          println(s"FTP uploading consumer started successfully: $msg")
          Right(msg)
