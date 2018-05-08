package fast.ssh.process

import fast.inventory.Host
import kotlinx.coroutines.experimental.Job
import net.schmizz.sshj.connection.channel.direct.Session
import fast.ssh.tryClose
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

class SshjProcess(
  val job: Job,
  internal val command: Session.Command,
  val host: Host
) : BasicProcess, Cancellable, Closeable {
  override fun close() {
    cancel()
  }

  override val startedAtMs: Long = System.currentTimeMillis()

  override fun kill() = TODO("not implemented")

  override val stdin: OutputStream = command.outputStream
  override val stderr: InputStream = command.errorStream
  override val stdout: InputStream = command.inputStream

  override fun isAlive(): Boolean = command.isOpen

  override fun isEOF(): Boolean = command.isEOF

  override fun getResult(console: Console, isTimeout: Boolean): ConsoleCommandResult {
    if(command.isOpen) {
      command.tryClose()
    }

    return ConsoleCommandResult(
      console,
      command.exitStatus,
      command.isEOF,
      isTimeout,
      System.currentTimeMillis() - startedAtMs)
  }

  override fun cancel() {
    if(command.isOpen) {
      command.tryClose()
    }

    job.cancel()
  }
}

