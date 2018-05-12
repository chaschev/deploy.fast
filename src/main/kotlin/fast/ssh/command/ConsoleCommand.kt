package fast.ssh.command

import fast.log.OkLogContext.Companion.okLog
import fast.log.slf4j.ok
import fast.ssh.IConsoleProcess
import fast.ssh.command.ConsoleLogging.outLogger
import fast.ssh.command.ConsoleLogging.sshErrLogger
import fast.ssh.command.ConsoleLogging.sshOutLogger
//import fast.ssh.command.ConsoleLogging.outLogger
//import fast.ssh.command.ConsoleLogging.sshErrLogger
//import fast.ssh.command.ConsoleLogging.sshOutLogger
import fast.ssh.process.Console
import org.slf4j.LoggerFactory

object ConsoleLogging {
  /* delivers session stdout to .out files */
  val sshOutLogger = okLog.getClassifiedLogger("ssh.out", "ssh.out")
  val sshErrLogger = okLog.getClassifiedLogger("ssh.err", "ssh.err")

  /* delivers session stdout to .log files */
  val outLogger = okLog.getClassifiedLogger("ssh.out.log", "ssh.out.log")
}

abstract class ConsoleCommand<T>(internal open val process: IConsoleProcess) {
  var printToOutput: Boolean = true

  val printingHandler: (Console) -> Unit = { console ->
//    print(console.newText())

    val outTrimmed = console.newOut.trim()
    val errTrimmed = console.newErr.trim()

    if(!outTrimmed.isEmpty()) outLogger.debug(process.host, outTrimmed)
    if(!errTrimmed.isEmpty()) outLogger.debug(process.host, errTrimmed)

    sshOutLogger.debug(process.host, console.newOut)
    sshErrLogger.debug(process.host, console.newErr)
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