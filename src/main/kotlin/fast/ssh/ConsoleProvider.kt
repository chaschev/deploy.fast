package fast.ssh

import fast.dsl.CommandLineResult
import fast.dsl.toFast
import fast.ssh.command.CommandResult
import fast.ssh.command.ProcessWithProcessing
import fast.ssh.command.script.ScriptCommandResult
import fast.ssh.command.script.ScriptDsl
import fast.ssh.files.Files
import fast.ssh.process.Console
import fast.ssh.process.ConsoleCommandResult
import java.io.Closeable

interface ConsoleProvider : Closeable {
    fun createSession(): ConsoleSession

    override fun close() {}

//    fun packages(): AptPackage = AptPackage(this)

  fun files(sudo: Boolean = false): Files
  fun user(): String
  val home: String
  fun address(): String
}

suspend fun ConsoleProvider.runAndWaitCustom(
  timeoutMs: Int,
  cmd: String
): CommandResult<Console> =
  runAndWaitProcess(cmd, { it }, timeoutMs = timeoutMs)

data class ConsoleProcessing<T>(
    val process: (Console) -> T,
    val processErrors: ((Console) -> T)? = null,
    val consoleHandler: ((console: Console) -> Unit)? = null
)

suspend fun <T> ConsoleProvider.execute(
  dsl: ScriptDsl<T>,
  timeoutMs: Int = 600000
): ScriptCommandResult<T> =
  dsl.asScript().execute(this, timeoutMs)

suspend fun ConsoleProvider.runAndWait(
  cmd: String,
  process: (Console) -> Boolean = { true },
  timeoutMs: Int = 60000
): CommandResult<Boolean> =
  runAndWaitProcess(cmd, process, { false }, timeoutMs)

suspend fun  ConsoleProvider.runResult(
  cmd: String,
  timeoutMs: Int = 60000
): CommandResult<ConsoleCommandResult> =
  runAndWaitProcess(cmd, { it.result }, timeoutMs = timeoutMs)

suspend fun <T> ConsoleProvider.runAndWaitProcess(
  cmd: String,
  process: (Console) -> T = { "ok" as T },
  processErrors: ((Console) -> T )? = null,
  timeoutMs: Int = 60000
): CommandResult<T> =
    runAndWaitInteractive(cmd, ConsoleProcessing(process, processErrors), timeoutMs)



suspend fun ConsoleProvider.run(
  cmd: String,
  timeoutMs: Int = 60000
): CommandLineResult<Boolean> = runAndWaitProcess(cmd, {it.result.isOk()}, {false}, timeoutMs = timeoutMs).toFast(false)

suspend fun <T> ConsoleProvider.runAndWaitInteractive(
  cmd: String,
  processing: ConsoleProcessing<T>,
  timeoutMs: Int
): CommandResult<T> =
    createSession().use { session ->
      val process = ProcessWithProcessing(
        process = session.plainCmd(cmd),
        processResult = processing.process,
        processErrors = processing.processErrors
      )

//      TODO: configure loggers
//      process.printToOutput = true

      process.runBlocking(timeoutMs, processing.consoleHandler)
    }

