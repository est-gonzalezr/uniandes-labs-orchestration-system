/** The ParserUtil object is responsible for providing utility functions for the
  * parsers.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package parsers

import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.*
import com.typesafe.config.ConfigException.IO

object ParserUtil:
  /** The folderAndRootFromBytes function is responsible for getting the folder
    * set and the root from the bytes of the file.
    *
    * @param fileBytes
    *   the bytes of the file
    *
    * @return
    *   either a string with the error message or a tuple with the folder set
    *   and the root
    */
  def foldersAndRootFromBytes(
      fileBytes: Array[Byte]
  ): Either[String, Tuple2[Set[String], String]] =
    val zipFile = zipFileFromBytes(fileBytes)
    zipFile match
      case Left(error) => Left(error)
      case Right(zipFile) =>
        val folders = zipFile.entries.asScala.toList
        val folderSet = folders.toSet.map(_.toString)
        val root = folders.headOption
          .map(
            _.getName
              .split("/")
              .headOption
              .getOrElse("")
          )
          .getOrElse("")

        Right(folderSet, root)

  /** The zipFileFromBytes function is responsible for creating a ZipFile from
    * the bytes of the file.
    *
    * @param fileBytes
    *   the bytes of the file
    *
    * @return
    *   either a string with the error message or a ZipFile
    */
  private def zipFileFromBytes(
      fileBytes: Array[Byte]
  ): Either[String, ZipFile] =
    try
      val tempFile = Files.createTempFile("temp", ".zip")
      Files.write(tempFile, fileBytes)

      Right(ZipFile(tempFile.toFile))
    catch case e: IOException => Left(s"IOException: ${e.getMessage}")
