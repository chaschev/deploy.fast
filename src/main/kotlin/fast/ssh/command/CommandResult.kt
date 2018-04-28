package fast.ssh.command

import fast.dsl.ext.nullForException
import fast.ssh.logger
import fast.ssh.process.Console
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

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
  override val console: Console
) : ICommandResult<T> {
  protected var _errors: MutableList<String>? = null

  override var value: T by InitLater()

  override var hasErrors: Boolean = nullForException { !console.result.isOk()} ?: true

  override fun toString(): String = "CommandResult(value=$value,console=$console)"

  protected fun errors(): MutableList<String> {
    if (_errors == null) _errors = ArrayList()
    return _errors!!
  }

  fun tryFindErrors(): CommandResult<T> {
    errors().addAll(Regexes.ERRORS.findAll(console.stdout).map { it.groups[0]!!.getLine(console.stdout) })
    errors().addAll(Regexes.ERRORS.findAll(console.stderr).map { it.groups[0]!!.getLine(console.stderr) })

    if (nullForException { console.result } == null) {
      errors().add("no result, the process could be running")
    } else
      if (console.result.isTimeout) {
        errors().add("timeout after ${console.result.timeMs}")
      }

    if(!errors().isEmpty()) hasErrors = true

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
    hasErrors = true
    return this
  }

  fun withSomeError(): CommandResult<T> {
    if(errors().isEmpty()) errors().add("there was some error, I am in an error handling branch")
    hasErrors = true
    return this
  }
}