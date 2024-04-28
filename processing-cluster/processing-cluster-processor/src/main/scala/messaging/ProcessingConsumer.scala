/** The ProcessingConsumer class is a consumer for the task processing queue. It
  * processes a file from the local filesystem and publishes a message to the
  * next queue. If a file can be processed then it publishes a message to the
  * next queue, if not it publishes a message to the fail queue.
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
    val ftp_file_path = messageMap("ftp_file_path")

    val fileBytes = FileManager.fileFromLocal(ftp_file_path) match
      case Left(error) =>
        handleError(error, messageMap, channel, failKey)
        Left(error)
      case Right(fileBytes) =>
        ParserUtil.foldersAndRootFromBytes(fileBytes) match
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

              handleDelete(ftp_file_path)
              Left(
                s"Missing necessary files or folder structure: $ftp_file_path"
              )
            else
              handleDelete(ftp_file_path)
              Right(fileBytes)

    fileBytes match
      case Left(error)      => println(s"Error processing file: $error")
      case Right(fileBytes) => handleSuccess(messageMap, channel, routingKey)

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
    val errorMap = messageMap + ("error" -> error) + ("status" -> "aborted")
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
    val successMap = messageMap + ("status" -> "success")
    val successString = write(successMap)
    publishFunction(channel, successString, routingKey)

  /** The handleDelete function deletes a file from the local filesystem.
    *
    * @param ftp_file_path
    *   the path of the file to delete
    */
  def handleDelete(
      ftp_file_path: String
  ): Unit =
    FileManager.deleteFile(ftp_file_path) match
      case Left(error) => println(s"Error deleting file: $error")
      case Right(_)    => println(s"File deleted: $ftp_file_path")
