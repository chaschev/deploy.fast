package fast.ssh.command

import fast.ssh.ConsoleSession
import fast.ssh.IConsoleProcess
import fast.ssh.plainCmd
import fast.ssh.process.Console

class LsCommand(override val process: IConsoleProcess) : ConsoleCommand<List<String>>(process) {
  init {
    printToOutput = false
  }

  override fun parseConsole(console: Console) =
    LsResult(console, console.stdout.split(Regexes.NEW_LINE))

  companion object {
    fun ls(folder: String, session: ConsoleSession): LsCommand =
      LsCommand(session.plainCmd("ls -ltra $folder"))
  }
}

data class LsResult(
  override val console: Console,
  override val value: List<String>?
) : CommandResult<List<String>>(console)



data class PwdResult(
  override val console: Console,
  override val value: String?
) : CommandResult<String>(console)


class PwdCommand(override val process: IConsoleProcess): ConsoleCommand<String>(process) {
  init { printToOutput = false }

  override fun parseConsole(console: Console) =
    PwdResult(console, console.stdout.trim().toString())

  companion object {
    fun pwd(session: ConsoleSession) = PwdCommand(session.plainCmd("pwd"))
  }
}


