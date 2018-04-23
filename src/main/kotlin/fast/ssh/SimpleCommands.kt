package fast.ssh

import fast.ssh.command.ConsoleCommand
import fast.ssh.command.JavaVersion

class SimpleCommands(val ssh: SshProvider) {
    suspend fun ls(folder: String, timeoutMs:Int = 5000): List<String>? =
        _helper(timeoutMs, {it.ls(folder)})

    private suspend fun <T>_helper(
        timeoutMs: Int,
        block: (session: CommonCommands) -> ConsoleCommand<T>): T? {

        return ssh.commonCommands().use {
            session ->
            block(session).runBlocking(timeoutMs).value
        }
    }
}