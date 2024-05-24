package executors

object MobileAppExecutor extends Executor:
  /** The execute function is responsible for executing the executor and its
    * necessary processes.
    *
    * @param localFilePath
    *   The local file path of the file to be executed.
    *
    * @return
    *   the name of the result file stored in the temp_files directory.
    */
  def execute(localFilePath: String): String =
    println("Executing MobileAppExecutor")
    "MobileAppExecutor"
