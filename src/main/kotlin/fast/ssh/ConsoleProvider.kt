package fast.ssh

import fast.dsl.CommandLineResult
import fast.dsl.toFast
import fast.ssh.command.CommandResult
import fast.ssh.command.ProcessConsoleCommand
import fast.ssh.command.ScriptDsl
import fast.ssh.command.ShellScript
import fast.ssh.files.Files
import fast.ssh.process.Console
import java.io.Closeable

interface ConsoleProvider : Closeable {
    fun createSession(): ConsoleSession

    override fun close() {}

//    fun packages(): AptPackage = AptPackage(this)

    fun files(): Files
}

suspend fun ConsoleProvider.runAndWaitCustom(
  timeoutMs: Int,
  cmd: String
): CommandResult<Console> =
  runAndWait(cmd, { it }, timeoutMs = timeoutMs)

data class ConsoleProcessing<T>(
    val process: (Console) -> T,
    val processErrors: ((Console) -> T)? = null,
    val consoleHandler: ((console: Console) -> Unit)? = null
)

suspend fun <T> ConsoleProvider.execute(
  dsl: ScriptDsl<T>,
  timeoutMs: Int = 60000
): CommandResult<T> =
  dsl.asScript().execute(this, timeoutMs)


suspend fun <T> ConsoleProvider.runAndWait(
  cmd: String,
  process: (Console) -> T = { "ok" as T },
  processErrors: ((Console) -> T )? = null,
  timeoutMs: Int = 60000
): CommandResult<T> =
    runAndWaitInteractive(cmd, ConsoleProcessing(process, processErrors), timeoutMs)

suspend fun <T> ConsoleProvider.runAndWait(
  cmd: String,
  processing: ConsoleProcessing<T>,
  timeoutMs: Int = 60000
): CommandResult<T>
    = runAndWaitInteractive(cmd, processing, timeoutMs)

suspend fun ConsoleProvider.run(
  cmd: String,
  timeoutMs: Int = 60000
): CommandLineResult<Boolean> = runAndWait(cmd, {it.result.isOk()}, {false}, timeoutMs = timeoutMs).toFast(false)

suspend fun <T> ConsoleProvider.runAndWaitInteractive(
  cmd: String,
  processing: ConsoleProcessing<T>,
  timeoutMs: Int
): CommandResult<T> =
    createSession().use { session ->

      ProcessConsoleCommand(
          process = session.plainCmd(cmd),
          processResult = processing.process,
          processErrors = processing.processErrors
        ).runBlocking(timeoutMs, processing.consoleHandler)
    }

