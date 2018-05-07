package fast.api

import fast.dsl.AggregatedValue
import fast.dsl.TaskResult
import fast.lang.nullForException
import fast.ssh.logger

interface ITaskResult<R> {
  val ok: Boolean
  val modified: Boolean

  // nullable for exception
  val value: R?

  val exception: Exception?

  fun valueNullable() = nullForException { value }

  operator fun plus(other: ITaskResult<Boolean>): TaskResult<Boolean> {
    return TaskResult(
      ok && other.ok,
      ok && other.ok,
      modified || other.modified
    )
  }


  operator fun times(other: ITaskResult<*>): ITaskResult<*> {
    return TaskResult(
      mergeValue(other),
      ok && other.ok,
      modified || other.modified
    )
  }

  // merges into an aggregated value
  fun mergeValue(other: ITaskResult<*>): Any {
    val v = value

    return if (v is AggregatedValue) {
      v.values.add(other.value)
      v.errors.add(other.exception)
    } else {
      AggregatedValue(
        arrayListOf(v as Any?, other.value),
        arrayListOf(exception, other.exception)
      )
    }
  }


  fun <O> mapValue(block: (R?) -> O) =
    TaskResult(block(value), this.ok, this.modified)

  fun asBoolean() = mapValue { ok }
  fun abortIfError(msg: String? = null, e: Exception? = null): ITaskResult<R> {
    return if(ok) this else {

      if(msg != null) {
        val s = "aborting due to result: $msg, $this"
        logger.warn { s }
      }

      if(e != null) throw e

      throw Exception("aborting due to result: $this")
    }
  }

  fun nullIfError(): ITaskResult<R>? = if(ok) this else null

  fun text(): String
}