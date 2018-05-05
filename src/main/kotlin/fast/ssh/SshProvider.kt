package fast.ssh

import fast.ssh.files.Files
import fast.ssh.files.SshFiles

interface SshProvider : ConsoleProvider {
    fun connect(): SshProvider

    @Deprecated("todo: remove", ReplaceWith("createSession().plainCmd()"))
    fun commonCommands() = createSession().asCommonCommands()

    override fun createSession(): GenericSshSession

    fun withSession(block: (session: GenericSshSession) -> Unit)
    fun runSimple(): SimpleCommands = SimpleCommands(this)

    override fun files(sudo: Boolean): Files = SshFiles(this, sudo)

    companion object {
      val dummy = object: SshProvider {
          override val home: String
              get() = TODO("not implemented")

          override fun user(): String {
              TODO("not implemented")
          }

          override fun address(): String {
              TODO("not implemented")
          }

          override fun connect(): SshProvider {
              TODO("dummy")
          }

          override fun createSession(): GenericSshSession {
              TODO("dummy")
          }

          override fun withSession(block: (session: GenericSshSession) -> Unit) {
              TODO("dummy")
          }

      }
    }
}

