package fast.ssh.files

import fast.log.OkLogging
import net.schmizz.sshj.xfer.FileSystemFile
import fast.ssh.*
import java.io.File
import java.time.Duration
import java.time.Instant


open class SshFiles(override val provider: ConsoleProvider, val _sudo: Boolean) : Files {
  private val sudo = if(_sudo) "sudo" else ""

  override suspend fun mkdirs(vararg paths: String): Boolean {
    return provider.runAndWaitProcess("$sudo mkdir -p ${paths.joinToString(" ")}", { true },
      { it.stdout.contains("exists") },
      5 * 1000).value
  }

  override suspend fun remove(vararg paths: String, recursive: Boolean): Boolean {
    val recursiveArg = if (recursive) "-r" else ""
    return provider.runAndWaitProcess("$sudo rm $recursiveArg ${paths.joinToString(" ")} || exit 0", { true },
      timeoutMs = 10 * 1000).value
  }

  companion object : OkLogging() {
    private val lsRegex = "^([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+(.{9,11}[^\\s]+)\\s+(.*)$".toRegex()
    internal fun logDuration(startedAt: Long, sizeKb: Int): String {
      val duration = Duration.between(Instant.ofEpochMilli(startedAt), Instant.now()).toMillis() + 1
      return "transfer done in ${duration / 1000}s, avg. speed ${sizeKb * 1000 / duration}kb/s"
    }
  }

  override suspend fun ls(path: String): List<RemoteFile> {
    return provider.runAndWaitProcess(
      cmd = "$sudo ls -ltrA $path",
      process = { console ->
        val list = console.stdout.trim().split('\n')
        val rows =
          if (list.isNotEmpty() && list[0].contains("total "))
            list.subList(1, list.size)
          else
            list

        val files = rows.map { line ->
          //-rw-r--r-- 1 root root 72945070 Oct 11 14:41 jenkins_2.73.2_all.deb
          //    1      2   3    4    5           6                  7
          val g = lsRegex.tryFind(line)!!

          try {
            val name = g[7]
            RemoteFileImpl(
              name = name,
              isFolder = g[1][0] == 'd',
              group = g[2],
              user = g[3]+":"+g[4],
              size = g[5].toLong(),
              path = "$path/$name",
              unixRights = g[1],
              lastModified = g[6]
            )
          } catch (e: Exception) {
            logger.warn { "ls parsing failed with line $line" }
            throw e
          }
        }

        files
      },
      processErrors = { emptyList() },
      timeoutMs = 15 * 1000
    ).value

  }

  override fun copyLocalFiles(dest: String, vararg files: File) {
    val startedAt = System.currentTimeMillis()

    val sizeKb = files.asSequence().sumBy { it.length().toInt() } / (1024)


    val prov = provider as GenericSshProvider
    val transfer = prov.sshClient.newSCPFileTransfer()
    val host = prov.host

    logger.info(host) {
      "uploading ${files.size} files to $dest, total: ${sizeKb}kb"
    }

    if (files.size == 1) {
      logger.info(host) { "transferring ${files[0]} to $dest" }

      transfer.upload(FileSystemFile(files[0]), dest)
    } else {
      for (file in files) {
        logger.info(host) { "transferring $file to $dest" }

        transfer.upload(FileSystemFile(file), dest + "/" + file.name)
      }
    }

    logger.info(host) { Companion.logDuration(startedAt, sizeKb) }

  }

  override fun copyRemoteFiles(destDir: File, vararg sourcePaths: String): List<File> {
    check(destDir.isDirectory)

    val prov = provider as GenericSshProvider
    val transfer = prov.sshClient.newSCPFileTransfer();

    val startedAt = System.currentTimeMillis()

    val files = sourcePaths.map { path ->
      logger.info(prov.host) { "transferring $path to ${destDir.absolutePath}" }

      val file = File(destDir, File(path).name)

      transfer.download(path, FileSystemFile(file))

      file
    }

    val finishedAt = System.currentTimeMillis()
    val sizeKb = files.sumBy { it.length().toInt() }

    logger.info {
      Companion.logDuration(startedAt, sizeKb)
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
    return provider.runAndWait("$sudo chmod ${if (recursive) "-R" else ""} $mod ${paths.joinToString(" ")}",
      timeoutMs = 15 * 1000).value
  }

  override suspend fun chown(vararg paths: String, owner: String, recursive: Boolean): Boolean {
    return provider.runAndWait("$sudo chown ${if (recursive) "-R" else ""} $owner ${paths.joinToString(" ")}",
      timeoutMs = 15 * 1000).value
  }

  override suspend fun ln(existingPath: String, linkPath: String): Boolean {
    return provider.runAndWait("$sudo ln -s $existingPath $linkPath",
      timeoutMs = 15 * 1000).value
  }

}