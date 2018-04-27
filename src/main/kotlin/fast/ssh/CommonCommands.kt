package fast.ssh

import fast.ssh.command.JavaVersionCommand
import fast.ssh.command.LsCommand
import fast.ssh.command.PwdCommand
import java.io.Closeable

class CommonCommands(val session: ConsoleSession) : Closeable {
fun ls(folder: String): LsCommand = LsCommand(session.plainCmd("ls -ltra $folder"))

  fun pwd(): PwdCommand = PwdCommand(session.plainCmd("pwd"))

  override fun close() {
    session.close()
  }
}