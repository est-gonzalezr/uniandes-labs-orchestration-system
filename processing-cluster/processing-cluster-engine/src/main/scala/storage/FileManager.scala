/** The FileManager object is responsible for managing the local file system. It
  * provides functions to write, read and delete files from the local file
  * system.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package storage

import os.Path
import os.read
import os.remove
import os.write

object FileManager:
  /** The temporalFileFolder function returns the path of the local directory to
    * store temporarily.
    *
    * @return
    *   the path of the local directory to store temporarily
    */
  def temporalFileFolder: Path = os.pwd / "temp_files"
  // def temporalFileFolder: Path = os.pwd / os.up / "temp_files"

  /** The pathToLocal function returns the path of the file in the local
    * directory.
    *
    * @param filePath
    *   the name of the file
    * @return
    *   the path of the file in the local directory
    */
  def pathToLocal(filePath: String): Path =
    os.Path(temporalFileFolder.toString + "/" + filePath)

  /** Writes a file to the local directory to store temporarily
    *
    * @param file
    *   the file to write
    * @param filePath
    *   the name of the file
    * @return
    *   Either a string with the error message or a string with the success
    *   message
    */
  def fileToLocal(
      file: Array[Byte],
      filePath: String
  ): Either[String, String] =
    try
      val path = pathToLocal(filePath)
      write(
        path,
        file,
        createFolders = true
      )
      Right(s"File written: $filePath")
    catch case e: Exception => Left(e.toString())

  /** Reads a file from the local directory
    *
    * @param filePath
    *   the name of the file to read
    * @return
    *   Either a string with the error message or the file as an array of bytes
    */
  def fileFromLocal(filePath: String): Either[String, Array[Byte]] =
    try
      val path = pathToLocal(filePath)
      val file = read.bytes(path)
      Right(file)
    catch case e: Exception => Left(e.toString())

  /** Deletes a file from the local directory
    *
    * @param filePath
    *   the name of the file to delete
    * @return
    *   Either a string with the error message or a string with the success
    *   message
    */
  def deleteFile(filePath: String): Either[String, String] =
    try
      val path = pathToLocal(filePath)
      remove(path)
      Right(s"File deleted: $filePath")
    catch case e: Exception => Left(e.toString())
