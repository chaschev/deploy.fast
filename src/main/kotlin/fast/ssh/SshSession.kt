package fast.ssh

import java.io.Closeable
import java.io.File

interface SshSession : ConsoleSession, Closeable {
    val provider: SshProvider
    fun upload(dest: String, vararg files: File)
}