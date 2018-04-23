package fast.ssh.command

import fast.ssh.process.Console

data class LsResult(
    override val console: Console,
    override val value: List<String>?
) : CommandResult<List<String>>(console)