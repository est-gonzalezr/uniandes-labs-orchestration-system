/** The ProcessingConsumer class is a consumer for the task processing queue. It
  * processes a file from the local filesystem and publishes a message to the
  * next queue. If a file can be processed then it publishes a message to the
  * next queue, if not it publishes a message to the fail queue.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package messaging.consumers

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Envelope
import executors.ElectricalGridExecutor
import executors.MobileAppExecutor
import executors.Printer3DExecutor
import executors.RoboticArmExecutor
import executors.WebAppExecutor
import parsers.ElectricalGridParser
import parsers.MobileAppParser
import parsers.ParserUtil
import parsers.Printer3DParser
import parsers.RoboticArmParser
import parsers.WebAppParser
import storage.FileManager
import types.OpaqueTypes.*
import types.TaskType
import upickle.default.read
import upickle.default.write

import java.io.File

private val ProcessingStatus = "status_processing"

/** The ProcessingConsumer class is a consumer for the task processing queue. It
  * processes a file from the local filesystem and publishes a message to the
  * next queue. If a file can be processed then it publishes a message to the
  * next queue, if not it publishes a message to the fail queue.
  *
  * @param channel
  *   the channel to use
  * @param routingKey
  *   the routing key to use
  * @param failKey
  *   the fail routing key to use
  * @param publishFunction
  *   the function to publish a message
  * @param taskType
  *   the type of task
  */
case class ProcessingConsumer(
    channel: Channel,
    routingKey: RoutingKey,
    failKey: RoutingKey,
    publishFunction: (Channel, String, RoutingKey) => Either[String, String],
    taskType: TaskType
) extends DefaultConsumer(channel):
  private val parser = taskType match
    case TaskType.Mobile         => MobileAppParser
    case TaskType.Web            => WebAppParser
    case TaskType.RoboticArm     => RoboticArmParser
    case TaskType.Printer3D      => Printer3DParser
    case TaskType.ElectricalGrid => ElectricalGridParser

  private val executor = taskType match
    case TaskType.Mobile         => MobileAppExecutor
    case TaskType.Web            => WebAppExecutor
    case TaskType.RoboticArm     => RoboticArmExecutor
    case TaskType.Printer3D      => Printer3DExecutor
    case TaskType.ElectricalGrid => ElectricalGridExecutor

  /** The handleDelivery function handles the delivery of a message from the
    * queue. It tries process a file from the local filesystem and depending on
    * the result it publishes a message to the next queue or to the fail queue.
    *
    * @param consumerTag
    * @param envelope
    * @param properties
    * @param body
    */
  override def handleDelivery(
      consumerTag: String,
      envelope: Envelope,
      properties: AMQP.BasicProperties,
      body: Array[Byte]
  ): Unit =
    val message = body.map(_.toChar).mkString
    println(s" [x] Received $message")

    // point of failure if any key or value is not a string
    val messageMap = read[Map[String, String]](body)
    val localFilePath = messageMap("local_file_path")

    val operationResult = FileManager.fileFromLocal(localFilePath) match
      case Left(error) =>
        handleError(error, messageMap, channel, failKey)
        Left(error)
      case Right(fileBytes) =>
        ParserUtil.foldersAndRootFromBytes(fileBytes, localFilePath) match
          case Left(error) =>
            handleError(error, messageMap, channel, failKey)
            Left(error)
          case Right((folderSet, root)) =>
            if !parser.hasNecessaryFiles(folderSet, root) then
              handleError(
                "Missing necessary files or folder structure",
                messageMap,
                channel,
                failKey
              )

              Left(
                s"Missing necessary files or folder structure: $localFilePath"
              )
            else
              val ftp_uploading_path = executor.execute(localFilePath)
              FileManager.fileFromLocal(ftp_uploading_path) match
                case Left(error) =>
                  handleError(error, messageMap, channel, failKey)
                  Left(error)
                case Right(_) =>
                  Right(
                    messageMap + ("ftp_uploading_path" -> ftp_uploading_path)
                  )

    operationResult match
      case Left(error) => println(s"Error processing file: $error")
      case Right(updatedMessageMap) =>
        handleSuccess(updatedMessageMap, channel, routingKey)

    handleLocalDelete(localFilePath)

    channel.basicAck(envelope.getDeliveryTag(), false)

  /** The handleError function handles an error by publishing a message with the
    * fail routing key.
    *
    * @param error
    *   the error message
    * @param messageMap
    *   the message map
    * @param channel
    *   the channel to use
    * @param failKey
    *   the fail routing key to use
    */
  def handleError(
      error: String,
      messageMap: Map[String, String],
      channel: Channel,
      failKey: RoutingKey
  ): Unit =
    val errorMap =
      messageMap + (ProcessingStatus -> "failed") + ("error" -> error)
    val errorString = write(errorMap)
    publishFunction(channel, errorString, failKey)

  /** The handleSuccess function handles a success by publishing a message with
    * the success routing key.
    *
    * @param messageMap
    *   the message map
    * @param channel
    *   the channel to use
    * @param routingKey
    *   the routing key to use
    */
  def handleSuccess(
      messageMap: Map[String, String],
      channel: Channel,
      routingKey: RoutingKey
  ): Unit =
    val successMap = messageMap + (ProcessingStatus -> "success")
    val successString = write(successMap)
    publishFunction(channel, successString, routingKey)

  /** The handleLocalDelete function deletes a file from the local filesystem.
    *
    * @param file_path
    *   the path of the file to delete
    */
  def handleLocalDelete(
      file_path: String
  ): Unit =
    FileManager.deleteFile(file_path) match
      case Left(error) =>
        println(s"Error deleting file from local filesystem: $error")
      case Right(_) =>
        println(s"File deleted from local filesystem: $file_path")
