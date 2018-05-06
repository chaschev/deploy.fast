package fast.ssh.command

//import io.vertx.core.Future
//import io.vertx.kotlin.coroutines.asFuture
import fast.ssh.IConsoleProcess
import fast.ssh.process.Console


abstract class ConsoleCommand<T>(internal open val process: IConsoleProcess) {
  var printToOutput: Boolean = true

  companion object {
    val printingHandler: (Console) -> Unit = { console ->
      print(console.newText())
    }
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
        {console: Console ->
          printingHandler(console)
          handler(console)
        }
      }
    }
}