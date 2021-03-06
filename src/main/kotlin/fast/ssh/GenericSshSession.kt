package fast.ssh

import fast.ssh.files.SshFiles
import fast.log.KLogging
import fast.log.OkLogging
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.xfer.FileSystemFile
import java.io.File

class GenericSshSession(override val provider: GenericSshProvider) : SshSession {
  companion object : OkLogging()

  internal var nativeSession: Session

  init {
    try {
      nativeSession = provider.sshClient.startSession()
      nativeSession.allocatePTY("vt100", 384, 24, 0, 0, emptyMap())
    } catch (e: Exception) {
      logger.info(e, { "could not start session ${provider.config} " })
      throw e
    }
  }

  override fun address(): String = provider.config.address
  override fun user(): String = provider.config.authUser

  override fun simpleCommand(cmd: String): IConsoleProcess =
    ConsoleSession.sshCommand(cmd, this)

  override fun close() {
    nativeSession.close()
  }

  override fun upload(dest: String, vararg files: File) {
    val startedAt = System.currentTimeMillis()

    val sizeKb = files.asSequence().sumBy { it.length().toInt() } / (1024)

    logger.info {
      "uploading ${files.size} files to $dest, total: ${sizeKb}kb"
    }

    val transfer = provider.sshClient.newSCPFileTransfer()

    if (files.size == 1) {
      logger.info(provider.host) { "transferring ${files[0]} to $dest" }

      transfer.upload(FileSystemFile(files[0]), dest)
    } else {
      for (file in files) {
        logger.info(provider.host) { "transferring $file to $dest" }

        transfer.upload(FileSystemFile(file), dest + "/" + file.name)
      }
    }

    logger.info {
      SshFiles.logDuration(startedAt, sizeKb)
    }
  }

}

class NativeSession : ConsoleSession {
  override fun address(): String {
    TODO("not implemented")
  }

  override fun user(): String {
    TODO("not implemented")
  }

  override fun simpleCommand(cmd: String): IConsoleProcess =
    ConsoleSession.command(cmd)

  companion object : OkLogging()

  override fun close() {
    //ok, not very good
    //that won't kill the process
  }
}
