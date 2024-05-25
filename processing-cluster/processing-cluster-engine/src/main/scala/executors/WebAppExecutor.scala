package executors

import storage.FileManager
import sys.process.Process

import java.io.File

object WebAppExecutor extends Executor:
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
    val tempDir = localFilePath.replace(".zip", "")
    val resultFileName = localFilePath.replace(".zip", "_processed.zip")

    val unzipping = Process(
      Seq(
        "unzip",
        localFilePath,
        "-d",
        tempDir
      ),
      new File(FileManager.temporalFileFolder.toString)
    ).!!

    println(unzipping)

    val cypressRun = Process(
      Seq(
        "cypress",
        "run",
        "--headless"
      ),
      new File(FileManager.pathToLocal(tempDir + "/cypress").toString)
    ).!!

    println(cypressRun)

    FileManager.fileToLocal(
      cypressRun.getBytes,
      tempDir + "/cypress/output.txt"
    )

    val zipped = Process(
      Seq(
        "zip",
        "-r",
        s"../../$resultFileName",
        "."
      ),
      new File(FileManager.pathToLocal(tempDir + "/cypress").toString)
    ).!!

    println(zipped)

    FileManager.deleteFolder(tempDir)

    resultFileName
