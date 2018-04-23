package fast.ssh.process

import kotlinx.coroutines.experimental.Job
import java.io.InputStream
import java.io.OutputStream

class NativeProcess(val job: Job, internal val process: Process) : BasicProcess, Cancellable {
    override val startedAtMs: Long = System.currentTimeMillis()

    override fun cancel()  {
        process.destroy()
        job.cancel()
    }


    override fun kill() {process.destroyForcibly()}

    override val stdin: OutputStream = process.outputStream
    override val stderr: InputStream = process.errorStream
    override val stdout: InputStream = process.inputStream

    override fun isAlive(): Boolean = process.isAlive
    override fun isEOF(): Boolean = !isAlive()

    override fun getResult(console: Console, isTimeout: Boolean): ConsoleCommandResult {
        return ConsoleCommandResult(console,
            if(process.isAlive) null else process.exitValue(),
            !process.isAlive,
            isTimeout,
            (System.currentTimeMillis() - startedAtMs).toInt()
            )
    }
}