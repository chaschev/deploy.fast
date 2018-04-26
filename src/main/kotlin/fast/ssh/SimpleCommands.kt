package fast.ssh

import fast.ssh.command.ConsoleCommand
import fast.ssh.command.JavaVersion

/* todo: check runAndWait */
class SimpleCommands(val ssh: SshProvider) {
  suspend fun ls(folder: String, timeoutMs: Int = 5000): List<String>? =
    _helper(timeoutMs, { it.ls(folder) })

  suspend fun pwd(timeoutMs: Int = 5000): String? =
    _helper(timeoutMs, { it.pwd() })

  private suspend fun <T> _helper(
    timeoutMs: Int,
    block: (session: CommonCommands) -> ConsoleCommand<T>): T? {

    return ssh.commonCommands().use { session ->
      block(session).runBlocking(timeoutMs).value
    }
  }
}