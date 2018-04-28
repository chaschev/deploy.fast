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

class LsResult(
  override val console: Console,
  value: List<String>
) : CommandResult<List<String>>(console) {
  init {withValue { value }}
}

class PwdResult(
  override val console: Console,
  value: String
) : CommandResult<String>(console) {
  init {
    withValue { value }
  }
}


class PwdCommand(override val process: IConsoleProcess): ConsoleCommand<String>(process) {
  init { printToOutput = false }

  override fun parseConsole(console: Console) =
    PwdResult(console, console.stdout.trim().toString())

  companion object {
    fun pwd(session: ConsoleSession) = PwdCommand(session.plainCmd("pwd"))
  }
}


