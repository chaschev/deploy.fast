package fast.ssh

import fast.ssh.command.CommandResult
import fast.ssh.command.ProcessWithProcessing
import fast.ssh.process.*
import java.io.Closeable
import java.io.File

interface ConsoleSession : Closeable {
  companion object {
    fun sshCommand(cmd: String, session: GenericSshSession): IConsoleProcess = SshjProcessEgg.newCommand(cmd, session)

    fun command(cmd: String, folder: File? = null): IConsoleProcess = NativeProcessEgg.command(cmd.split(" ").toList(), folder)
  }

  fun run(process: IConsoleProcess, timeoutMs: Int,
          callback: (console: Console) -> Unit = {}): IConsoleProcess {

    return process
      .start(timeoutMs, callback)
  }

  fun simpleCommand(cmd: String): IConsoleProcess

  fun asCommonCommands(): CommonCommands = CommonCommands(this)

  fun address(): String
  fun user(): String
}

fun ConsoleSession.plainCmd(cmd: String): IConsoleProcess {
  return when (this) {
    is GenericSshSession -> ConsoleSession.sshCommand(cmd, this)
    is NativeSession -> ConsoleSession.command(cmd)
    else -> TODO()
  }
}

suspend fun <T> ConsoleSession.runAndWait(
  timeoutMs: Int,
  cmd: String,
  process: (Console) -> T,
  processErrors: ((Console) -> T)? = null
): CommandResult<T> = ProcessWithProcessing(plainCmd(cmd), process, processErrors).runBlocking(timeoutMs)

suspend fun ConsoleSession.runAndWaitCustom(timeoutMs: Int,
                                            cmd: String): CommandResult<Console> =
  runAndWait(timeoutMs, cmd, { it })




