/** The Main object is the entry point of the processing service. It starts the
  * processing instances.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

import com.rabbitmq.client.Channel
import configuration.ConfigurationUtil.configProcessingConsumerQuantity
import configuration.ConfigurationUtil.configTaskType
import configuration.ConfigurationUtil.envProcessingConsumerQuantity
import configuration.ConfigurationUtil.envTaskType
import messaging.MessagingUtil
import messaging.TaskProcessingManager
import types.TaskType

val DefaultProcessingConsumerQuantity = 5

/** The main function is responsible for starting the processing service.
  *
  * @param args
  *   the arguments passed to the program
  */
@main def main(args: String*): Unit =
  val executeLocal = args.contains("--local")
  val configurationResult = executeConfigurations(!executeLocal)
  val consumerQuantity =
    (if executeLocal then configProcessingConsumerQuantity
     else envProcessingConsumerQuantity).toOption
      .map(_.toInt)
      .getOrElse(DefaultProcessingConsumerQuantity)
  val taskType =
    (if executeLocal then configTaskType
     else envTaskType)
      .map(taskMapping)
      .getOrElse(TaskType.Mobile)

  configurationResult match
    case Right(channel) =>
      startProcessingConsumers(channel, consumerQuantity, TaskType.Mobile)
    case Left(error) =>
      println(s"Error: $error")

  while true do Thread.sleep(10000)

/** The executeConfigurations function is responsible for executing the
  * configurations for the processing service.
  *
  * @param dockerEnv
  *   a boolean indicating if the service is running in a docker environment
  *
  * @return
  *   either a string with the error message or a channel
  */
def executeConfigurations(dockerEnv: Boolean = true): Either[String, Channel] =
  println("Configuring local processor messaging...")
  val channelWithConfig =
    for
      conn <- MessagingUtil.connectionFromContext(dockerEnv)
      channel <- MessagingUtil.channelDefinition(conn)
      _ <- TaskProcessingManager
        .configureTaskProcessingMessaging(channel)
    yield channel

  channelWithConfig match
    case Left(error) =>
      println(s"Error: $error")
      Left(error)
    case Right(channel) =>
      println("Local processor messaging configured successfully")
      Right(channel)

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
  *   a unit indicating the result of the function
  */
def startProcessingConsumers(
    channel: Channel,
    quantity: Int,
    taskType: TaskType
): Unit =
  println(s"Starting $quantity processing consumers...")

  for _ <- 1 to quantity do
    val consumer =
      TaskProcessingManager.createProcessingConsumer(
        channel,
        taskType
      )

    TaskProcessingManager.consumeTaskProcessingQueue(
      channel,
      false,
      consumer
    ) match
      case Left(error) =>
        println(s"Error starting a processing consumer: $error")
      case Right(msg) =>
        println(s"Processing consumer started successfully: $msg")

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
