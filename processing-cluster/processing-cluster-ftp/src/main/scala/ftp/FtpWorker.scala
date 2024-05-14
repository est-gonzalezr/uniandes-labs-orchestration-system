/** The ftp package contains the FtpWorker class and its companion object. The
  * FtpWorker class is responsible for uploading and downloading files to and
  * from the ftp server. The companion object is responsible for reading the
  * configurations from the application.conf file and creating the FtpWorker
  * objects. If the service is running in a docker environment, the
  * configurations are read from the environment variables.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package ftp

import com.typesafe.config.ConfigException.IO
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.ConfigException.WrongType
import com.typesafe.config.ConfigFactory
import configuration.ConfigurationUtil.configFtpHost
import configuration.ConfigurationUtil.configFtpPassword
import configuration.ConfigurationUtil.configFtpPort
import configuration.ConfigurationUtil.configFtpUsername
import configuration.ConfigurationUtil.envFtpHost
import configuration.ConfigurationUtil.envFtpPassword
import configuration.ConfigurationUtil.envFtpPort
import configuration.ConfigurationUtil.envFtpUsername
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPConnectionClosedException

import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.io.InputStream
import java.io.ByteArrayInputStream

/** The FtpWorker class is responsible for downloading files from the ftp
  * server.
  *
  * @param host
  *   the host of the ftp server
  * @param port
  *   the port of the ftp server
  * @param username
  *   the username
  * @param password
  *   the password
  */
case class FtpWorker(
    host: String,
    port: Int,
    username: String,
    password: String
):
  val client = FTPClient()

/** Companion object of the Client class. Responsible for reading configurations
  * of the server and creating clients.
  */
object FtpWorker:
  private val config = ConfigFactory.load()

  /** Creates a new client with the configurations from the application.conf.
    *
    * @param dockerEnv
    *   a boolean indicating if the service is running in a docker environment
    *
    * @return
    *   either a string with the error message or the client
    */
  def apply(dockerEnv: Boolean = true): Either[String, FtpWorker] =
    val client =
      dockerEnv match
        case true =>
          for
            host <- envFtpHost
            port <- envFtpPort
            username <- envFtpUsername
            password <- envFtpPassword
          yield FtpWorker(host, port, username, password)
        case false =>
          for
            host <- configFtpHost
            port <- configFtpPort
            username <- configFtpUsername
            password <- configFtpPassword
          yield FtpWorker(host, port, username, password)

    try
      client match
        case Left(error) => Left(error)
        case Right(client) =>
          client.client.connect(client.host, client.port)
          client.client.login(client.username, client.password)
          client.client.enterLocalPassiveMode()
          Right(client)
    catch
      case e: FTPConnectionClosedException =>
        Left("FTPConnectionClosedException: FTP Connection closed")
      case e: UnknownHostException =>
        Left("UnknownHostException: Couldn't connect to the server")
      case e: SocketException =>
        Left("SocketException: Couldn't set socket timeout")
      case e: IOException => Left(s"IOException: ${e.getMessage()}")
      case e: Exception   => Left(s"Exception: ${e.getMessage()}")

  /** Gets the file from the server and returns it as an array of bytes.
    * @param client
    *   the client to use to connect to the server
    * @param path
    *   the path of the file to retrieve
    *
    * @return
    *   Either a string with the error message or the file as an array of bytes
    */
  def fileFromPath(
      ftpWorker: FtpWorker,
      path: String
  ): Either[String, Array[Byte]] =
    try
      val file = ftpWorker.client.retrieveFileStream(path)

      if file == null then Left(s"File not found: $path")
      else
        val fileData = Right(file.readAllBytes())
        file.close()
        ftpWorker.client.completePendingCommand()
        fileData
    catch
      case e: IOException =>
        Left(s"IOException: Couldn't retrieve file: ${e.getMessage()}")
      case e: Exception =>
        Left(s"Exception: Couldn't retrieve file: ${e.getMessage()}")

  /** Stores a file in the server.
    *
    * @param ftpWorker
    *   the client to use to connect to the server
    * @param path
    *   the path of the file to store
    * @param file
    *   the file as an array of bytes
    *
    * @return
    *   Either a string with the error message or a boolean
    */
  def fileToPath(
      ftpWorker: FtpWorker,
      path: String,
      file: Array[Byte]
  ): Either[String, true] =
    try
      // turn array of bytes into input stream
      val inputStream: InputStream = new ByteArrayInputStream(file)
      val result = ftpWorker.client.storeFile(path, inputStream)
      if !result then Left(s"Couldn't store file: $path")
      else Right(true)
    catch
      case e: IOException =>
        Left(s"IOException: Couldn't store file: ${e.getMessage()}")

  /** Sends a NoOp command to the server to keep the connection alive.
    * @param ftpWorker
    *   the client to use to connect to the server
    *
    * @return
    *   Either a string with the error message or a boolean
    */
  def sendNoOp(ftpWorker: FtpWorker): Either[String, true] =
    try
      val result = ftpWorker.client.sendNoOp()
      if !result then Left("NoOp failed")
      else Right(true)
    catch
      case e: IOException =>
        Left(s"IOException: NoOp failed: ${e.getMessage()}")

  /** Deletes a file from the server.
    *
    * @param ftpWorker
    *   the client to use to connect to the server
    * @param path
    *   the path of the file to delete
    *
    * @return
    *   Either a string with the error message or a boolean
    */
  def deleteFromPath(
      ftpWorker: FtpWorker,
      path: String
  ): Either[String, true] =
    try
      val result = ftpWorker.client.deleteFile(path)
      if !result then Left(s"Couldn't delete file: $path")
      else Right(true)
    catch
      case e: IOException =>
        Left(s"IOException: Couldn't delete file: ${e.getMessage()}")
      case e: Exception =>
        Left(s"Exception: Couldn't delete file: ${e.getMessage()}")
