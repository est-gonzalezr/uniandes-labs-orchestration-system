/** The ftp package contains the FtpDownloader class and its companion object.
  * The FtpDownloader class is responsible for downloading files from the ftp
  * server. The companion object is responsible for reading the configurations
  * from the application.conf file and creating the FtpDownloader objects. If
  * the service is running in a docker environment, the configurations are read
  * from the environment variables.
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

/** The FtpDownloader class is responsible for downloading files from the ftp
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
case class FtpDownloader(
    host: String,
    port: Int,
    username: String,
    password: String
):
  val client = FTPClient()

/** Companion object of the Client class. Responsible for reading configurations
  * of the server and creating clients.
  */
object FtpDownloader:
  private val config = ConfigFactory.load()

  /** Creates a new client with the configurations from the application.conf.
    *
    * @param dockerEnv
    *   a boolean indicating if the service is running in a docker environment
    *
    * @return
    *   either a string with the error message or the client
    */
  def apply(dockerEnv: Boolean = true): Either[String, FtpDownloader] =
    val client =
      dockerEnv match
        case true =>
          for
            host <- envFtpHost
            port <- envFtpPort
            username <- envFtpUsername
            password <- envFtpPassword
          yield FtpDownloader(host, port, username, password)
        case false =>
          for
            host <- configFtpHost
            port <- configFtpPort
            username <- configFtpUsername
            password <- configFtpPassword
          yield FtpDownloader(host, port, username, password)

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
      case e: Exception   => Left(s"Exception: e.getMessage()")

  /** Gets the file from the server and returns it as an array of bytes.
    * @param client
    *   the client to use to connect to the server
    * @param path
    *   the path of the file to retrieve
    * @return
    *   Either a string with the error message or the file as an array of bytes
    */
  def fileFromPath(
      downloader: FtpDownloader,
      path: String
  ): Either[String, Array[Byte]] =
    val file = downloader.client.retrieveFileStream(path)

    val server_response =
      if file == null then Left(s"File not found: $path")
      else
        val fileData = Right(file.readAllBytes())
        file.close()
        downloader.client.completePendingCommand()
        fileData
    server_response

  /** Sends a NoOp command to the server to keep the connection alive.
    * @param downloader
    *   the client to use to connect to the server
    * @return
    *   Either a string with the error message or a boolean
    */
  def sendNoOp(downloader: FtpDownloader): Either[String, Boolean] =
    try
      val result = downloader.client.sendNoOp()
      Right(result)
    catch
      case e: IOException =>
        Left(s"IOException: NoOp failed: ${e.getMessage()}")
