package fast.ssh.command

import fast.ssh.process.Console
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class InitLater
(val finalize: Boolean = true) {
  operator fun <T> getValue(obj: Any, property: KProperty<*>): T {
    val x = property.javaField?.get(obj)

    return x as? T ?: throw Exception("property $property is not initialized")
  }

  operator fun <T> setValue(obj: Any, property: KProperty<*>, value: T) {
    if(finalize) {
      val currentValue = property.javaField?.get(obj)

      require(currentValue == null, {"value is already set for property $property"})
    }

    property.javaField!!.set(obj, value)
  }
}

open class CommandResult<T>(
  override val console: Console,
  override val hasOutputErrors: Boolean = false
) : ICommandResult<T> {
  var errors: MutableList<String>? = null

  override var value: T by InitLater()

  override fun toString(): String = "CommandResult(value=$value,console=$console)"

  fun tryFindErrors(): CommandResult<T> {
    if (errors == null) errors = ArrayList()


    errors!!.addAll(Regexes.ERRORS.findAll(console.stdout).map { it.groups[0]!!.getLine(console.stdout) })
    errors!!.addAll(Regexes.ERRORS.findAll(console.stderr).map { it.groups[0]!!.getLine(console.stderr) })

    if (console.result == null) {
      errors!!.add("no result, the process could be running")
    } else
      if (console.result!!.isTimeout) {
        errors!!.add("timeout after ${console.result!!.timeMs}")
      }

    return this
  }
}