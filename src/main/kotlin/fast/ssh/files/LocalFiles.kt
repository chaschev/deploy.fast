package fast.ssh.files

import fast.ssh.ConsoleProvider
import java.io.File

class LocalFiles(override val provider: ConsoleProvider): SshFiles(provider, false) {
    override fun copyLocalFiles(dest: String, vararg files: File) {
        TODO("implement local copy")
    }
}