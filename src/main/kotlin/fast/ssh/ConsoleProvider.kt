package fast.ssh

import fast.ssh.command.CommandResult
import fast.ssh.command.ProcessConsoleCommand
import fast.ssh.files.Files
import fast.ssh.process.Console
import java.io.Closeable

interface ConsoleProvider : Closeable {
    fun createSession(): ConsoleSession

    override fun close() {}

//    fun packages(): AptPackage = AptPackage(this)

    fun files(): Files
}

suspend fun ConsoleProvider.runAndWaitCustom(timeoutMs: Int,
                                             cmd: String): CommandResult<Console> =
    runAndWait(timeoutMs, cmd, { it })

data class ConsoleProcessing<T>(
    val process: (Console) -> T,
    val processErrors: ((Console) -> T)? = null,
    val consoleHandler: ((console: Console) -> Unit)? = null
)

suspend fun <T> ConsoleProvider.runAndWait(
    timeoutMs: Int,
    cmd: String,
    process: (Console) -> T,
    processErrors: ((Console) -> T)? = null
): CommandResult<T> =
    runAndWaitInteractive(timeoutMs, cmd, ConsoleProcessing(process, processErrors))

suspend fun <T> ConsoleProvider.runAndWait(
    timeoutMs: Int,
    cmd: String,
    processing: ConsoleProcessing<T>
): CommandResult<T>
    = runAndWaitInteractive(timeoutMs, cmd, processing)

suspend fun <T> ConsoleProvider.runAndWaitInteractive(
    timeoutMs: Int,
    cmd: String,
    processing: ConsoleProcessing<T>
): CommandResult<T> =
    createSession().use { session ->
        ProcessConsoleCommand(
          session.plainCmd(cmd), processing.process, processing.processErrors)
            .runBlocking(timeoutMs, processing.consoleHandler)
    }

