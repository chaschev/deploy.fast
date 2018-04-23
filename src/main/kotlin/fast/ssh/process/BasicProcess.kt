package fast.ssh.process

import java.io.InputStream
import java.io.OutputStream

interface BasicProcess : Cancellable {
    val startedAtMs: Long
    val stdin: OutputStream
    val stderr: InputStream
    val stdout: InputStream
    fun kill()
    fun getResult(console: Console, isTimeout: Boolean = false): ConsoleCommandResult
    fun isAlive(): Boolean
    fun isEOF(): Boolean
}

