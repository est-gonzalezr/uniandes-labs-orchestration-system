/** Basic trait for all parsers so that they can be changed at runtime.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package parsers

trait Parser:
  /** The hasNecessaryFiles function is responsible for checking if the folder
    * has the necessary files.
    *
    * @param folderSet
    *   the set of files in the folder
    *
    * @param root
    *   the root of the folder
    *
    * @return
    *   a boolean indicating if the folder has the necessary files
    */
  def hasNecessaryFiles(folderSet: Set[String], root: String): Boolean
