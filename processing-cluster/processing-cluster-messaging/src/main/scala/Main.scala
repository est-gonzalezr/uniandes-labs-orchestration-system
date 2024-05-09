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
import messaging.ClusterMessagingManager
import messaging.MessagingUtil

/** The main function configures the local and global parsing clusters and
  * starts the parsing cluster consumers.
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
    case Right(localChannel) =>
      startClusterConsumer(localChannel) match
        case Left(error) =>
          println("Cluster consumer not started successfully: Shutting down...")
        case Right(msg) =>
          println("Cluster consumer started successfully: Running...")
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
  println("Configuring local cluster and global messaging...")

  val localChannel: Either[String, Channel] =
    for
      conn <- MessagingUtil.connectionFromContext(dockerEnv)
      channel <- MessagingUtil.channelDefinition(conn)
      _ <- ClusterMessagingManager.configureLocalClusterMessaging(
        channel
      )
    yield channel

  localChannel match
    case Right(localChannel) =>
      println("Local messaging configured successfully")
      Right(localChannel)
    case Left(localError) =>
      println(s"Local configuration error: $localError")
      Left(localError)

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
