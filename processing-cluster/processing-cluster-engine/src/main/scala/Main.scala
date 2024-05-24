/** The Main object is the entry point of the local parsing cluster. It
  * configures the local and global parsing clusters and starts the parsing
  * cluster consumers. It also handles the errors that may occur during the
  * configuration of the channels. This is the starting point of a local parsing
  * cluster.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

import com.rabbitmq.client.Channel
import configuration.ConfigurationUtil.configFtpDownloadingConsumerQuantity
import configuration.ConfigurationUtil.configFtpUploadingConsumerQuantity
import configuration.ConfigurationUtil.configProcessingConsumerQuantity
import configuration.ConfigurationUtil.configTaskType
import configuration.ConfigurationUtil.envFtpDownloadingConsumerQuantity
import configuration.ConfigurationUtil.envFtpUploadingConsumerQuantity
import configuration.ConfigurationUtil.envProcessingConsumerQuantity
import configuration.ConfigurationUtil.envTaskType
import messaging.ClusterMessagingManager
import messaging.MessagingUtil
import types.TaskType

val DefaultFtpDownloadingConsumerQuantity = 2
val DefaultFtpUploadingConsumerQuantity = 2
val DefaultProcessingConsumerQuantity = 5

/** The main function configures the local and global parsing clusters and
  * starts the parsing cluster consumers.
  *
  * @param args
  *   the arguments passed to the program
  */
@main def main(args: String*): Unit =
  val executeLocal = args.contains("--local")
  val configurationResult = executeConfigurations(!executeLocal)

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

  val processingConsumerQuantity =
    (if executeLocal then configProcessingConsumerQuantity
     else envProcessingConsumerQuantity).toOption
      .map(_.toInt)
      .filter(_ > 0)
      .getOrElse(DefaultProcessingConsumerQuantity)

  val taskType =
    (if executeLocal then configTaskType
     else envTaskType)
      .map(taskMapping)
      .getOrElse(TaskType.Web)

  configurationResult match
    case Left(error) =>
      println(s"Error: $error")
    case Right(localChannel) =>
      val clusterConsumerStartupState = startClusterConsumer(localChannel)

      val downloadingConusmersStartupState = startFtpDownloadingConsumers(
        localChannel,
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

      val uploadingConsumersStartupState = startFtpUploadingConsumers(
        localChannel,
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

      val processingConsumersStartupState =
        startProcessingConsumers(
          localChannel,
          processingConsumerQuantity,
          taskType
        ).foldLeft(
          Right(
            "All processing consumers started successfully: Running..."
          ): Either[
            String,
            String
          ]
        )((acc, consumerState) =>
          consumerState match
            case Left(_) =>
              Left(
                "Not all processing consumers started successfully: Shutting down..."
              )
            case Right(_) =>
              acc
        )

      val allConsumers = Seq(
        clusterConsumerStartupState,
        downloadingConusmersStartupState,
        uploadingConsumersStartupState,
        processingConsumersStartupState
      )

      allConsumers.foldLeft(
        Right("All consumers started successfully: Running..."): Either[
          String,
          String
        ]
      )((acc, consumerState) =>
        consumerState match
          case Left(_) =>
            Left("Not all consumers started successfully: Shutting down...")
          case Right(_) =>
            acc
      ) match
        case Left(error) =>
          println(s"Error: $error")
        case Right(msg) =>
          println(msg)
          while true do Thread.sleep(10000)

/** The executeConfigurations function configures the local and global parsing
  * clusters and starts the parsing cluster consumers.
  *
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  *
  * @return
  *   either a string with the error message or a channel
  */
def executeConfigurations(
    dockerEnv: Boolean = true
): Either[String, Channel] =
  println("Configuring cluster messaging...")

  val localChannel: Either[String, Channel] =
    for
      conn <- MessagingUtil.connectionFromContext(dockerEnv)
      channel <- MessagingUtil.channelDefinition(conn)
      _ <- ClusterMessagingManager.configureLocalClusterMessaging(
        channel
      )
    yield channel

  localChannel match
    case Left(localError) =>
      println(s"Cluster messaging configuration error: $localError")
      Left(localError)
    case Right(localChannel) =>
      println("Cluster messaging configured successfully")
      Right(localChannel)

/** The startClusterConsumer function starts the parsing cluster consumer that
  * consume the global parsing queue and publish the messages to the local
  * system.
  *
  * @param localChannel
  *   the local channel
  */
def startClusterConsumer(
    localChannel: Channel
): Either[String, String] =
  println(s"Starting cluster consumer...")

  val consumer =
    ClusterMessagingManager.createClusterConsumer(
      localChannel
    )

  ClusterMessagingManager.consumeGlobalQueue(
    localChannel,
    false,
    consumer
  ) match
    case Left(error) =>
      println(s"Error starting the cluster consumer: $error")
      Left(s"Error starting the cluster consumer: $error")
    case Right(msg) =>
      println(s"Cluster consumer started successfully: $msg")
      Right(s"Cluster consumer started successfully: $msg")

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
    *   a sequence of either a string with the error message or a string with
    *   the success message
    */
def startFtpDownloadingConsumers(
    channel: Channel,
    quantity: Int,
    dockerEnv: Boolean = true
): IndexedSeq[Either[String, String]] =
  println(s"Starting $quantity ftp downloading consumers...")

  for _ <- 1 to quantity
  yield ClusterMessagingManager.createDownloadingConsumer(
    channel,
    dockerEnv
  ) match
    case Left(error) =>
      println(error)
      Left(error)
    case Right(consumer) =>
      ClusterMessagingManager.consumeTaskDownloadingQueue(
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
  yield ClusterMessagingManager.createUploadingConsumer(
    channel,
    dockerEnv
  ) match
    case Left(error) =>
      println(error)
      Left(error)
    case Right(consumer) =>
      ClusterMessagingManager.consumeTaskUploadingQueue(
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

  /** The startProcessingConsumers function is responsible for starting the
    * procesing consumers.
    *
    * @param channel
    *   the channel to use for the processing consumers
    * @param quantity
    *   the quantity of consumers to start
    * @param taskType
    *   the type of task to process
    *
    * @return
    *   a sequence of either a string with the error message or a string with
    *   the success message
    */
def startProcessingConsumers(
    channel: Channel,
    quantity: Int,
    taskType: TaskType
): IndexedSeq[Either[String, String]] =
  println(s"Starting $quantity processing consumers...")

  for _ <- 1 to quantity
  yield
    val consumer =
      ClusterMessagingManager.createProcessingConsumer(channel, taskType)
    ClusterMessagingManager.consumeTaskProcessingQueue(
      channel,
      false,
      consumer
    ) match
      case Left(error) =>
        println(s"Error starting a processing consumer: $error")
        Left(error)
      case Right(msg) =>
        println(s"Processing consumer started successfully: $msg")
        Right(msg)

/** The taskMapping function maps a string to a TaskType.
  *
  * @param taskstr
  *   the string to map
  * @return
  *   the TaskType
  */
def taskMapping(taskstr: String): TaskType =
  taskstr match
    case "Mobile"         => TaskType.Mobile
    case "Web"            => TaskType.Web
    case "RoboticArm"     => TaskType.RoboticArm
    case "Printer3D"      => TaskType.Printer3D
    case "ElectricalGrid" => TaskType.ElectricalGrid
    case _                => TaskType.Mobile
