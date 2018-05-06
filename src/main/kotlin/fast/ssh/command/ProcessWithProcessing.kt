package fast.ssh.command

import fast.ssh.IConsoleProcess
import fast.ssh.process.Console

/**
 * ProcessErrors may throw an exception and it is fine
 * Otherwise default handler will try to find errors and will add them or an 'empty' error
 *
 */
class ProcessWithProcessing<T>(
  override val process: IConsoleProcess,
  private val processResult: (Console) -> T,
  private val processErrors: ((Console) -> T)? = null
) : ConsoleCommand<T>(process) {

  override fun parseConsole(console: Console): CommandResult<T> =
    CommandResult<T>(console).withValue { processResult(console) }


  override fun processError(e: Exception?): CommandResult<T> {
    return if (processErrors == null) {
      super.processError(e)
    } else {
      CommandResult<T>(process.console)
        .withValue { processErrors.invoke(process.console) }
        .withSomeError()
    }
  }
}