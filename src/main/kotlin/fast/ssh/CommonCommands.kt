package fast.ssh

import fast.ssh.command.JavaVersionCommand
import fast.ssh.command.LsCommand
import fast.ssh.command.PwdCommand
import java.io.Closeable

class CommonCommands(val session: ConsoleSession) : Closeable {
  companion object {
    /**
     * This is a universal and simple command
     *
     * todo: can also make native process from Line<String>
     */
  }


  fun ls(folder: String): LsCommand = LsCommand(session.plainCmd("ls -ltra $folder"))

  fun pwd(): PwdCommand = PwdCommand(session.plainCmd("pwd"))

  fun javaVersion(): JavaVersionCommand =
    JavaVersionCommand(session.plainCmd("java -version"))

  override fun close() {
    session.close()
  }
}