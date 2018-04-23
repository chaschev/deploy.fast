package fast.ssh.command

import fast.ssh.IConsoleProcess
import fast.ssh.process.Console

class ProcessConsoleCommand<T>(
  override val process: IConsoleProcess,
  private val processResult: (Console) -> T,
  private val processErrors: ((Console) -> T)? = null
) : ConsoleCommand<T>(process) {

    override fun parseConsole(console: Console): CommandResult<T> =
        CommandResult(console, processResult(console))

    override fun processError(): CommandResult<T> {
        return if (processErrors == null) {
            super.processError()
        } else {
            CommandResult(process.console, processErrors!!(process.console))
        }
    }
}