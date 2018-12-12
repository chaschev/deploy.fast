package fast.ssh.process

import fast.inventory.Host
import kotlinx.coroutines.Job
import fast.ssh.ConsoleProcess
import fast.ssh.GenericSshSession
import honey.lang.joinSpace
import java.io.File

interface ProcessEgg {
  fun giveBirth(job: Job): BasicProcess
  val host: Host
  val cmd: String
}

class SshjProcessEgg(override val cmd: String, val session: GenericSshSession) : ProcessEgg {
  companion object {
    fun newCommand(cmd: String, session: GenericSshSession): ConsoleProcess =
      ConsoleProcess(SshjProcessEgg(cmd, session))
  }

  override val host
    get() = session.provider.host

  override fun giveBirth(job: Job): SshjProcess = SshjProcess(job, session.nativeSession.exec(cmd), session.provider.host)

  override fun toString(): String =
    "$cmd, ip=${session.provider.config.address}"
}

class NativeProcessEgg(
  override val cmd: String,
  val processBuilder: ProcessBuilder
) : ProcessEgg {

  override val host: Host
    get() = Host.local


  companion object {
    fun command(command: List<String>, folder: File? = null): ConsoleProcess =
      ConsoleProcess(process(command, folder))

    fun process(command: List<String>, folder: File? = null): NativeProcessEgg =
      NativeProcessEgg(command.joinSpace(), ProcessBuilder(command).directory(folder))
  }

  override fun giveBirth(job: Job): BasicProcess =
    NativeProcess(job, processBuilder.start())

  override fun toString(): String = cmd
}