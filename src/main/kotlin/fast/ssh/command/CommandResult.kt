package fast.ssh.command

import fast.ssh.process.Console

open class CommandResult<T>(
    override val console: Console,
    override val value: T?,
    override val hasOutputErrors: Boolean = false
) : ICommandResult<T> {
    var errors: MutableList<String>? = null

    override fun toString(): String
        = "CommandResult(value=$value,console=$console)"

    fun tryFindErrors(): CommandResult<T> {
        if(errors == null) errors = ArrayList()

        errors!!.addAll(Regexes.ERRORS.findAll(console.stdout).map { it.groups[0]!!.getLine(console.stdout) })
        errors!!.addAll(Regexes.ERRORS.findAll(console.stderr).map { it.groups[0]!!.getLine(console.stderr) })

        if(console.result == null) {
            errors!!.add("no result, the process could be running")
        } else
        if(console.result!!.isTimeout) {
            errors!!.add("timeout after ${console.result!!.timeMs}")
        }

        return this
    }
}