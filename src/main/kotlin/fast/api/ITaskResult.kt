package fast.api

import fast.dsl.AggregatedValue
import fast.dsl.TaskResult

interface ITaskResult<R> {
  val ok: Boolean
  val modified: Boolean
  val value: R

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
      v.list.add(other.value as Any)
      v
    } else {
      AggregatedValue(v as Any, other.value as Any)
    }
  }


  fun <O> mapValue(block: (R) -> O) =
    TaskResult(block(value), this.ok, this.modified)

  fun text(): String
}