/** Basic trait for all executors so that they can be changed at runtime.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package executors

trait Executor:

  /** The execute function is responsible for executing the executor and its
    * necessary processes.
    *
    * @param localFilePath
    *   The local file path of the file to be executed.
    *
    * @return
    *   the name of the result file stored in the temp_files directory.
    */
  def execute(localFilePath: String): String
