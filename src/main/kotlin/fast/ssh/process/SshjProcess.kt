package fast.ssh.process

//import io.vertx.core.AsyncResult
//import io.vertx.core.Closeable
//import io.vertx.core.Handler
import kotlinx.coroutines.experimental.Job
import net.schmizz.sshj.connection.channel.direct.Session
import fast.ssh.tryClose
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

class SshjProcess(val job: Job, internal val command: Session.Command) : BasicProcess, Cancellable, Closeable {
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
    return ConsoleCommandResult(console,
      command.exitStatus,
      command.isEOF,
      isTimeout,
      System.currentTimeMillis() - startedAtMs)
  }

  override fun cancel() {
    command.tryClose()
    job.cancel()
  }

//    override fun close(completionHandler: Handler<AsyncResult<Void>>?) {
//        newCommand.tryClose()
//    }
}

