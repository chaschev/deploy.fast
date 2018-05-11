package fast.ssh.command

import fast.ssh.IConsoleProcess
//import fast.ssh.command.ConsoleLogging.outLogger
//import fast.ssh.command.ConsoleLogging.sshErrLogger
//import fast.ssh.command.ConsoleLogging.sshOutLogger
import fast.ssh.process.Console
import org.slf4j.LoggerFactory

/*
open class KLogging : KLoggable {
  override val logger: KLogger = logger()
}
*/

object ConsoleLogging {
  /* delivers session stdout to .out files */
//  val sshOutLogger = LoggerFactory.getLogger("ssh.out")
//  val sshErrLogger = LoggerFactory.getLogger("ssh.err")

  /* delivers session stdout to .log files */
//  val outLogger = LoggerFactory.getLogger("ssh.out.log")
}

abstract class ConsoleCommand<T>(internal open val process: IConsoleProcess) {
  var printToOutput: Boolean = true

  val printingHandler: (Console) -> Unit = { console ->
//    print(console.newText())

    val outTrimmed = console.newOut.trim()
    val errTrimmed = console.newErr.trim()



    //TODO FIX if(!outTrimmed.isEmpty()) outLogger.debug(process.host.marker, outTrimmed)
    //TODO FIX if(!errTrimmed.isEmpty()) outLogger.debug(process.host.marker, errTrimmed)

    //TODO FIX sshOutLogger.debug(process.host.marker, console.newOut)
    //TODO FIX sshErrLogger.debug(process.host.marker, console.newErr)
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