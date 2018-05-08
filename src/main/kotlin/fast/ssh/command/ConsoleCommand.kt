package fast.ssh.command

import fast.ssh.IConsoleProcess
import fast.ssh.command.ConsoleLogging.sshErrLogger
import fast.ssh.command.ConsoleLogging.sshOutLogger
import fast.ssh.process.Console
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.MarkerManager


object ConsoleLogging {
  val sshOutLogger = LogManager.getLogger("ssh.out")
  val sshErrLogger = LogManager.getLogger("ssh.err")


  val SSH_MARKER = MarkerManager.getMarker("ssh")
  val SSH_ERR_MARKER = MarkerManager.getMarker("ssh_err").setParents(SSH_MARKER)
  val SSH_OUT_MARKER = MarkerManager.getMarker("ssh_out").setParents(SSH_MARKER)
}

abstract class ConsoleCommand<T>(internal open val process: IConsoleProcess) {
  var printToOutput: Boolean = true

  val printingHandler: (Console) -> Unit = { console ->
//    print(console.newText())

    sshOutLogger.debug(process.host.marker, console.newIn)
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