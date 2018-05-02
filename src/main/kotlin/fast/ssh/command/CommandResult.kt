package fast.ssh.command

import fast.api.ext.nullForException
import fast.ssh.logger
import fast.ssh.process.Console
import kotlin.reflect.KProperty

class InitLater(val finalize: Boolean = true) {
  private var value: Any? = null

  operator fun <T> getValue(obj: Any, property: KProperty<*>): T {
    if(value == null) throw Exception("property $property is not initialized")
    return value!! as T
  }

  operator fun <T> setValue(obj: Any, property: KProperty<*>, value: T) {
    if(finalize) {
      require(this.value == null, {"value is already set for property $property"})
    }

    this.value = value
  }
}

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

    errors().addAll(Regexes.ERRORS.findAll(console.stdout).map { it.groups[0]!!.getLine(console.stdout) })
    errors().addAll(Regexes.ERRORS.findAll(console.stderr).map { it.groups[0]!!.getLine(console.stderr) })

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