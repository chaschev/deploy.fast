package fast.ssh.command

import fast.lang.InitLater
import fast.lang.nullForException
import fast.ssh.logger
import fast.ssh.process.Console


open class CommandResult<T>(
  override val console: Console,
  override var exception: Exception? = null
) : ICommandResult<T> {
  protected var _errors: MutableList<String>? = null

  override var value: T by InitLater()

  override fun toString(): String = "CommandResult(value=$value,console=$console)"

  override fun errors(): MutableList<String> {
    if (_errors == null) _errors = ArrayList()
    return _errors!!
  }

  fun tryFindErrors(): CommandResult<T> {
    if(exception != null) {
      errors().add("exception: $exception")
    }

    errors().addAll(Regexes.ERRORS.findAll(console.stdout).map { it.groups[0]!!.getLineWithMe(console.stdout) })
    errors().addAll(Regexes.ERRORS.findAll(console.stderr).map { it.groups[0]!!.getLineWithMe(console.stderr) })

    if (nullForException { console.result } == null) {
      errors().add("no result, the process could be running")
    } else
      if (console.result.isTimeout) {
        errors().add("timeout after ${console.result.timeMs}")
      }

    return this
  }

  inline fun withValue(value: () -> T): CommandResult<T> {
    try {
      this.value = value()
    } catch (e: Exception) {
      logger.warn("exception during parsing result", e)
      withError("exception during parsing result, ${e.message}") }
    return this
  }

  fun withError(s: String): CommandResult<T> {
    errors().add(s)
    return this
  }

  fun withSomeError(): CommandResult<T> {
    if(errors().isEmpty()) errors().add("there was some error but we can't understand which exactly, I am in an error handling branch")
    return this
  }
}