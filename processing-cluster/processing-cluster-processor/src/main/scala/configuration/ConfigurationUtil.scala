/** The ConfigurationUtil object contains the functions to read the local
  * RabbitMQ configuration from the environment variables or the
  * application.conf file.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package configuration

import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.ConfigException.WrongType
import com.typesafe.config.ConfigFactory

private val EnvLocalRabbitMqHostStr = "LOCAL_RABBITMQ_HOST"
private val EnvLocalRabbitMqPortStr = "LOCAL_RABBITMQ_PORT"
private val EnvLocalRabbitMqUsernameStr = "LOCAL_RABBITMQ_USERNAME"
private val EnvLocalRabbitMqPasswordStr = "LOCAL_RABBITMQ_PASSWORD"

private val EnvFtpHost = "FTP_HOST"
private val EnvFtpPort = "FTP_PORT"
private val EnvFtpUsername = "FTP_USERNAME"
private val EnvFtpPassword = "FTP_PASSWORD"

private val EnvTaskTypeStr = "TASK_TYPE"

private val EnvFtpConsumerQuantityStr = "FTP_CONSUMER_QUANTITY"
private val EnvProcessingConsumerQuantityStr = "PROCESSING_CONSUMER_QUANTITY"

private val ConfigLocalRabbitMqHostStr = "local_rabbitmq_host"
private val ConfigLocalRabbitMqPortStr = "local_rabbitmq_port"
private val ConfigLocalRabbitMqUsernameStr = "local_rabbitmq_username"
private val ConfigLocalRabbitMqPasswordStr = "local_rabbitmq_password"

private val ConfigFtpHost = "ftp_host"
private val ConfigFtpPort = "ftp_port"
private val ConfigFtpUsername = "ftp_username"
private val ConfigFtpPassword = "ftp_password"

private val ConfigTaskTypeStr = "task_type"

private val ConfigFtpConsumerQuantityStr = "ftp_consumer_quantity"
private val ConfigProcessingConsumerQuantityStr = "processing_consumer_quantity"

/** The ConfigurationUtil object contains the functions to read the local
  * configuration from the environment variables or the application.conf file.
  */
object ConfigurationUtil:
  private val config = ConfigFactory.load()

  /** The envRabbitmqHost function reads the local RabbitMQ host from the
    * environment variables.
    *
    * @return
    *   either the local RabbitMQ host or an error message
    */
  def envRabbitmqHost: Either[String, String] =
    sys.env
      .get(EnvLocalRabbitMqHostStr)
      .toRight(s"Missing: $EnvLocalRabbitMqHostStr not found in env")

  /** The envRabbitmqPort function reads the local RabbitMQ port from the
    * environment variables.
    *
    * @return
    *   either the local RabbitMQ port or an error message
    */
  def envRabbitmqPort: Either[String, Int] =
    sys.env.get(EnvLocalRabbitMqPortStr) match
      case Some(port) =>
        port.toIntOption.toRight(
          s"WrongType: $EnvLocalRabbitMqPortStr has wrong type"
        )
      case None => Left(s"Missing: $EnvLocalRabbitMqPortStr not found in env")

  /** The envRabbitmqUsername function reads the local RabbitMQ username from
    * the environment variables.
    *
    * @return
    *   either the local RabbitMQ username or an error message
    */
  def envRabbitmqUsername: Either[String, String] =
    sys.env
      .get(EnvLocalRabbitMqUsernameStr)
      .toRight(s"Missing: $EnvLocalRabbitMqUsernameStr not found in env")

  /** The envRabbitmqPassword function reads the local RabbitMQ password from
    * the environment variables.
    *
    * @return
    *   either the local RabbitMQ password or an error message
    */
  def envRabbitmqPassword: Either[String, String] =
    sys.env
      .get(EnvLocalRabbitMqPasswordStr)
      .toRight(s"Missing: $EnvLocalRabbitMqPasswordStr not found in env")

  /** The envFtpHost function reads the FTP host from the environment variables.
    *
    * @return
    *   either the FTP host or an error message
    */
  def envFtpHost: Either[String, String] =
    sys.env
      .get(EnvFtpHost)
      .toRight(s"Missing: $EnvFtpHost not found in env")

  /** The envFtpPort function reads the FTP port from the environment variables.
    *
    * @return
    *   either the FTP port or an error message
    */
  def envFtpPort: Either[String, Int] =
    sys.env.get(EnvFtpPort) match
      case Some(port) =>
        port.toIntOption.toRight(s"WrongType: $EnvFtpPort has wrong type")
      case None => Left(s"Missing: $EnvFtpPort not found in env")

  /** The envFtpUsername function reads the FTP username from the environment
    * variables.
    *
    * @return
    *   either the FTP username or an error message
    */
  def envFtpUsername: Either[String, String] =
    sys.env
      .get(EnvFtpUsername)
      .toRight(s"Missing: $EnvFtpUsername not found in env")

  /** The envFtpPassword function reads the FTP password from the environment
    * variables.
    *
    * @return
    *   either the FTP password or an error message
    */
  def envFtpPassword: Either[String, String] =
    sys.env
      .get(EnvFtpPassword)
      .toRight(s"Missing: $EnvFtpPassword not found in env")

  /** The envTaskType function reads the task type from the environment
    * variables.
    *
    * @return
    *   either the task type or an error message
    */
  def envTaskType: Either[String, String] =
    sys.env
      .get(EnvTaskTypeStr)
      .toRight(s"Missing: $EnvTaskTypeStr not found in env")

  /** The envFtpConsumerQuantity function reads the FTP consumer quantity from
    * the environment variables.
    *
    * @return
    *   either the FTP consumer quantity or an error message
    */
  def envFtpConsumerQuantity: Either[String, Int] =
    sys.env.get(EnvFtpConsumerQuantityStr) match
      case Some(quantity) =>
        quantity.toIntOption.toRight(
          s"WrongType: $EnvFtpConsumerQuantityStr has wrong type"
        )
      case None => Left(s"Missing: $EnvFtpConsumerQuantityStr not found in env")

  /** The envProcessingConsumerQuantity function reads the processing consumer
    * quantity from the environment variables.
    *
    * @return
    *   either the processing consumer quantity or an error message
    */
  def envProcessingConsumerQuantity: Either[String, Int] =
    sys.env.get(EnvProcessingConsumerQuantityStr) match
      case Some(quantity) =>
        quantity.toIntOption.toRight(
          s"WrongType: $EnvProcessingConsumerQuantityStr has wrong type"
        )
      case None =>
        Left(
          s"Missing: $EnvProcessingConsumerQuantityStr not found in env"
        )

  /** The configRabbitmqHost function reads the local RabbitMQ host from the
    * application.conf file.
    *
    * @return
    *   either the local RabbitMQ host or an error message
    */
  def configRabbitmqHost: Either[String, String] =
    try Right(config.getString(ConfigLocalRabbitMqHostStr))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigLocalRabbitMqHostStr not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigLocalRabbitMqHostStr has wrong type")

  /** The configRabbitmqPort function reads the local RabbitMQ port from the
    * application.conf file.
    *
    * @return
    *   either the local RabbitMQ port or an error message
    */
  def configRabbitmqPort: Either[String, Int] =
    try Right(config.getInt(ConfigLocalRabbitMqPortStr))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigLocalRabbitMqPortStr not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigLocalRabbitMqPortStr has wrong type")

  /** The configRabbitmqUsername function reads the local RabbitMQ username from
    * the application.conf file.
    *
    * @return
    *   either the local RabbitMQ username or an error message
    */
  def configRabbitmqUsername: Either[String, String] =
    try Right(config.getString(ConfigLocalRabbitMqUsernameStr))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigLocalRabbitMqUsernameStr not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigLocalRabbitMqUsernameStr has wrong type")

  /** The configRabbitmqPassword function reads the local RabbitMQ password from
    * the application.conf file.
    *
    * @return
    *   either the local RabbitMQ password or an error message
    */
  def configRabbitmqPassword: Either[String, String] =
    try Right(config.getString(ConfigLocalRabbitMqPasswordStr))
    catch
      case e: Missing =>
        Left(
          s"Missing: $ConfigLocalRabbitMqPasswordStr not found in config"
        )
      case e: WrongType =>
        Left(s"WrongType: $ConfigLocalRabbitMqPasswordStr has wrong type")

  /** The configFtpHost function reads the FTP host from the application.conf
    * file.
    *
    * @return
    *   either the FTP host or an error message
    */
  def configFtpHost: Either[String, String] =
    try Right(config.getString(ConfigFtpHost))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigFtpHost not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigFtpHost has wrong type")

  /** The configFtpPort function reads the FTP port from the application.conf
    * file.
    *
    * @return
    *   either the FTP port or an error message
    */
  def configFtpPort: Either[String, Int] =
    try Right(config.getInt(ConfigFtpPort))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigFtpPort not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigFtpPort has wrong type")

  /** The configFtpUsername function reads the FTP username from the
    * application.conf file.
    *
    * @return
    *   either the FTP username or an error message
    */
  def configFtpUsername: Either[String, String] =
    try Right(config.getString(ConfigFtpUsername))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigFtpUsername not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigFtpUsername has wrong type")

  /** The configFtpPassword function reads the FTP password from the
    * application.conf file.
    *
    * @return
    *   either the FTP password or an error message
    */
  def configFtpPassword: Either[String, String] =
    try Right(config.getString(ConfigFtpPassword))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigFtpPassword not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigFtpPassword has wrong type")

  /** The configTaskType function reads the task type from the application.conf
    * file.
    *
    * @return
    *   either the task type or an error message
    */
  def configTaskType: Either[String, String] =
    try Right(config.getString(ConfigTaskTypeStr))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigTaskTypeStr not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigTaskTypeStr has wrong type")

  /** The configFtpConsumerQuantity function reads the FTP consumer quantity
    * from the application.conf file.
    *
    * @return
    *   either the FTP consumer quantity or an error message
    */
  def configFtpConsumerQuantity: Either[String, Int] =
    try Right(config.getInt(ConfigFtpConsumerQuantityStr))
    catch
      case e: Missing =>
        Left(s"Missing: $ConfigFtpConsumerQuantityStr not found in config")
      case e: WrongType =>
        Left(s"WrongType: $ConfigFtpConsumerQuantityStr has wrong type")

  /** The configProcessingConsumerQuantity function reads the processing
    * consumer quantity from the application.conf file.
    *
    * @return
    *   either the processing consumer quantity or an error message
    */
  def configProcessingConsumerQuantity: Either[String, Int] =
    try Right(config.getInt(ConfigProcessingConsumerQuantityStr))
    catch
      case e: Missing =>
        Left(
          s"Missing: $ConfigProcessingConsumerQuantityStr not found in config"
        )
      case e: WrongType =>
        Left(s"WrongType: $ConfigProcessingConsumerQuantityStr has wrong type")
