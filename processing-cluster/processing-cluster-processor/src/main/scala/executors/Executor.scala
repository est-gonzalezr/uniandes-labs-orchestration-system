/** Basic traito for all executors so that they can be changed at runtime.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package executors

trait Executor:

  /** The execute function is responsible for executing the executor and its
    * necessary processes.
    */
  def execute(): Unit
