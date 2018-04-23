package fast.ssh

import mu.KLogging
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.xfer.FileSystemFile
import fast.ssh.process.*
import fast.ssh.process.SshjProcessMom.Companion.command
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal

class GenericSshSession(override val provider: GenericSshProvider) : SshSession {
    companion object : KLogging()

    internal var nativeSession: Session

    init {
        try {
            nativeSession = provider.sshClient.startSession()
            nativeSession.allocatePTY("vt100", 384, 24, 0, 0, emptyMap())
        } catch (e: Exception) {
            logger.info(e, {"could not start session ${provider.config} "})
            throw e
        }
    }

    override fun simpleCommand(cmd: String): IConsoleProcess =
        ConsoleSession.sshCommand(cmd, this)

    internal fun runCommand(
        cmd: String,
        callback: (console: Console) -> Unit = {},
        timeoutMs: Int = 60000
    ): IConsoleProcess {
        logger.info { "running ```${cmd.cuteCut(30)}```" }

        val command = command(cmd, this)

        return run(command, timeoutMs, callback)
    }

    override fun close() {
        nativeSession.close()
    }

    override fun upload(dest: String, vararg files: File) {
        val startedAt = System.currentTimeMillis()

        val sizeKb = files.asSequence().sumBy { it.length().toInt() } / (1024)

        logger.info {
            "uploading ${files.size} files to $dest, total: ${sizeKb}kb" }

        val transfer = provider.sshClient.newSCPFileTransfer()

        if (files.size == 1) {
            logger.info { "transferring ${files[0]} to $dest" }

            transfer.upload(FileSystemFile(files[0]), dest)
        } else {
            for (file in files) {
                logger.info { "transferring $file to $dest" }

                transfer.upload(FileSystemFile(file), dest + "/" + file.name)
            }
        }

        logger.info { val duration = Duration.between(Instant.ofEpochMilli(startedAt), Instant.now()).getSeconds()
            "done in $duration, avg. speed ${sizeKb / duration}kb/s" }
    }

}

class NativeSession : ConsoleSession {
    override fun simpleCommand(cmd: String): IConsoleProcess =
        ConsoleSession.command(cmd)

    companion object : KLogging()

    override fun close() {
        //ok, not very good
        //that won't kill the process
    }
}
