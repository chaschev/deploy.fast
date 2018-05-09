package fast.ssh.command

import fast.ssh.IConsoleProcess
import fast.ssh.command.ConsoleLogging.outLogger
import fast.ssh.command.ConsoleLogging.sshErrLogger
import fast.ssh.command.ConsoleLogging.sshOutLogger
import fast.ssh.process.Console
import org.apache.logging.slf4j.Log4jMarkerFactory
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

/*
open class KLogging : KLoggable {
  override val logger: KLogger = logger()
}
*/

object ConsoleLogging {
  val sshOutLogger = LoggerFactory.getLogger("ssh.out")
  val outLogger = LoggerFactory.getLogger("log.out")
  val sshErrLogger = LoggerFactory.getLogger("ssh.err")
  val markerFactory = Log4jMarkerFactory()

  val SSH_MARKER = MarkerFactory.getMarker("ssh")
  val SSH_ERR_MARKER = MarkerFactory.getMarker("ssh_err").apply {add(SSH_MARKER)}
  val SSH_OUT_MARKER = MarkerFactory.getMarker("ssh_out").apply { SSH_MARKER.add(this)}

}

abstract class ConsoleCommand<T>(internal open val process: IConsoleProcess) {
  var printToOutput: Boolean = true

  val printingHandler: (Console) -> Unit = { console ->
//    print(console.newText())

    val outTrimmed = console.newOut.trim()
    val errTrimmed = console.newErr.trim()

    if(!outTrimmed.isEmpty()) outLogger.debug(process.host.marker, outTrimmed)
    if(!errTrimmed.isEmpty()) outLogger.debug(process.host.marker, errTrimmed)

    sshOutLogger.debug(process.host.marker, console.newOut)
    sshErrLogger.debug(process.host.marker, console.newErr)
  }

  suspend fun runBlocking(timeoutMs: Int = 60000, handler: ((Console) -> Unit)? = null): CommandResult<T> {
    val activeHandler = chooseHandler(handler)
    val process = process.start(timeoutMs, activeHandler)

    process.await()

    return processWhenDone(process)
  }


  protected fun processWhenDone(process: IConsoleProcess): CommandResult<T> {
    return if (process.result?.isOk() == true) {
      try {
        parseConsole(process.console)
      } catch (e: Exception) {
        processError(e)
      }
    } else {
      processError(null)
    }
  }

  protected open fun processError(exception: Exception?): CommandResult<T> {
    return CommandResult<T>(process.console, exception).tryFindErrors().withSomeError()
  }

  abstract fun parseConsole(console: Console): CommandResult<T>

//    fun async(timeoutMs: Int = 60000, handler: ((Console) -> Unit)? = null): Future<CommandResult<T>> {
//        return process.start(timeoutMs, chooseHandler(handler)).job.asFuture().map {
//            processWhenDone(process)
//        }
//    }

  private fun chooseHandler(handler: ((Console) -> Unit)?) =
    if (handler == null)
      (if (printToOutput) printingHandler else null)
    else {
      if (!printToOutput)
        handler
      else {
        { console: Console ->
          printingHandler(console)
          handler(console)
        }
      }
    }
}