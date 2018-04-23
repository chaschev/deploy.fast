package fast.ssh.command

import fast.ssh.ConsoleSession
import fast.ssh.IConsoleProcess
import fast.ssh.plainCmd
import fast.ssh.process.Console

class LsCommand(override val process: IConsoleProcess): ConsoleCommand<List<String>>(process) {
    init { printToOutput = false }

    override fun parseConsole(console: Console) =
        LsResult(console, console.stdout.split(Regexes.NEW_LINE))

    companion object {
        fun ls(folder: String, session: ConsoleSession): LsCommand =
            LsCommand(session.plainCmd("ls -ltra $folder"))
    }
}

