package parsers

object WebAppParser extends Parser:
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
  def hasNecessaryFiles(folderSet: Set[String], root: String): Boolean =
    val necessaryFiles = Set(
      s"$root/cypress.config.js"
    )

    necessaryFiles.forall(folderSet.contains)
