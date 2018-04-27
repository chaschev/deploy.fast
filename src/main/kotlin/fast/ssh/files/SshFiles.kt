package fast.ssh.files

import net.schmizz.sshj.xfer.FileSystemFile
import fast.ssh.*
import java.io.File
import java.time.Duration
import java.time.Instant


open class SshFiles(override val provider: ConsoleProvider) : Files {
  override suspend fun mkdirs(vararg paths: String): Boolean {
    return provider.runAndWait("mkdir -p ${paths.joinToString(" ")}", { true },
      { it.stdout.contains("exists") },
      5 * 1000).value!!

  }

  companion object {
    private val lsRegex = "^([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+(.{9,11}[^\\s]+)\\s+.*$".toRegex()
  }

  override suspend fun ls(path: String): List<RemoteFile> {
    return provider.runAndWait("ls -ltra $path", { console ->
      val list = console.stdout.split('\n')
      val rows =
        if (list.isNotEmpty() && list[0].contains("total "))
          list.subList(1, list.size)
        else
          list

      val files = rows.map { line ->
        //-rw-r--r-- 1 root root 72945070 Oct 11 14:41 jenkins_2.73.2_all.deb
        //    0      1   2    3    4       5       6     7
        val g = lsRegex.tryFind(line)!!

        RemoteFileImpl(
          name = g[7],
          isFolder = g[0][0] == 'd',
          group = g[2],
          user = g[3],
          size = g[4].toLong(),
          path = path,
          unixRights = g[0],
          lastModified = g[5]
        )
      }

      files
    }, timeoutMs = 5 * 1000).value!!

  }

  override fun copyLocalFiles(dest: String, vararg files: File) {
    val startedAt = System.currentTimeMillis()

    val sizeKb = files.asSequence().sumBy { it.length().toInt() } / (1024)

    logger.info {
      "uploading ${files.size} files to $dest, total: ${sizeKb}kb"
    }

    val transfer = (provider as GenericSshProvider).sshClient.newSCPFileTransfer()

    if (files.size == 1) {
      logger.info { "transferring ${files[0]} to $dest" }

      transfer.upload(FileSystemFile(files[0]), dest)
    } else {
      for (file in files) {
        logger.info { "transferring $file to $dest" }

        transfer.upload(FileSystemFile(file), dest + "/" + file.name)
      }
    }

    logger.info {
      val duration = Duration.between(Instant.ofEpochMilli(startedAt), Instant.now()).seconds
      "done in $duration, avg. speed ${sizeKb / duration}kb/s"
    }

  }

  override fun copyRemoteFiles(destDir: File, vararg sourcePaths: String): List<File> {
    check(destDir.isDirectory)

    val transfer = (provider as GenericSshProvider).sshClient.newSCPFileTransfer();

    val startedAt = System.currentTimeMillis()

    val files = sourcePaths.map { path ->
      logger.info { "transferring $path to ${destDir.absolutePath}" }

      val file = File(destDir, File(path).name)

      transfer.download(path, FileSystemFile(file))

      file
    }

    val finishedAt = System.currentTimeMillis()
    val sizeKb = files.sumBy { it.length().toInt() }

    logger.info {
      val duration = Duration.between(Instant.ofEpochMilli(startedAt), Instant.ofEpochMilli(finishedAt)).seconds
      "download done in $duration, avg. speed ${sizeKb / duration}kb/s"
    }

    return files
  }

  override fun readAsString(path: String): String {
    val tempFile = File.createTempFile("honey", "badger")
    tempFile.delete()

    val myFile = copyRemoteFiles(tempFile.parentFile, path).first()

    val s = myFile.readText()

    myFile.delete()

    return s
  }

  override fun writeToString(path: String, s: String) {
    val tempFile = File.createTempFile("honey", "badger")

    tempFile.writeText(s)

    copyLocalFiles(path, tempFile)

    tempFile.delete()
  }

  override suspend fun chmod(vararg paths: String, mod: String, recursive: Boolean): Boolean {
    return provider.runAndWait("chmod ${if (recursive) "-R" else ""} $mod ${paths.joinToString(" ")}",
      { true },
      timeoutMs = 5 * 1000).value == true
  }

  override suspend fun chown(vararg paths: String, owner: String, recursive: Boolean): Boolean {
    return provider.runAndWait("chown ${if (recursive) "-R" else ""} $owner ${paths.joinToString(" ")}",
      { true },
      timeoutMs = 5 * 1000).value == true
  }

  override suspend fun ln(existingPath: String, linkPath: String): Boolean {
    return provider.runAndWait("ln -s $existingPath $linkPath",
      { true },
      timeoutMs = 5 * 1000).value == true
  }

}