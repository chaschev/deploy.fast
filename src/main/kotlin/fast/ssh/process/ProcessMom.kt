package fast.ssh.process

import kotlinx.coroutines.experimental.Job
import fast.ssh.ConsoleProcess
import fast.ssh.GenericSshSession
import java.io.File

interface ProcessMom {
    fun giveBirth(parentJob: Job): BasicProcess
}

class SshjProcessMom(val cmd: String, val session: GenericSshSession) : ProcessMom {
    companion object {
        fun newCommand(cmd: String, session: GenericSshSession): ConsoleProcess =
            ConsoleProcess(SshjProcessMom(cmd, session))
    }

    override fun giveBirth(parentJob: Job): SshjProcess
        = SshjProcess(parentJob, session.nativeSession.exec(cmd))

    override fun toString(): String =
        "`$cmd` @${session.provider.config.address}"
}

class NativeProcessMom(val processBuilder: ProcessBuilder) : ProcessMom {
    companion object {
        fun command(command: List<String>, folder: File? = null): ConsoleProcess =
            ConsoleProcess(process(command, folder))

        fun process(command: List<String>, folder: File? = null) : NativeProcessMom =
            NativeProcessMom(ProcessBuilder(command).directory(folder))
    }

    override fun giveBirth(parentJob: Job): BasicProcess =
        NativeProcess(parentJob, processBuilder.start())

    override fun toString(): String =
        processBuilder.command().joinToString (" ")
}