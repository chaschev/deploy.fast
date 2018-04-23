package fast.dsl

import kotlinx.coroutines.experimental.Job

class GlobalSessionRuntimeContext {

}

class SessionRuntimeContext(
  val task: Task,
  val ssh: Any
) {
  internal var parent: SessionRuntimeContext? = null

  private val children = ArrayList<SessionRuntimeContext>()

  private val stats = TaskStats()

  @Volatile
  private var job: Job? = null
}

class TaskStats (
  var startMs: Long = 0,
  var endMs: Long = 0,
  var exitCode: Int = -1
) {
  val counters = TaskStatsCounters()
}

class TaskStatsCounters(
  var errors: Int = 0,
  var modifications: Int = 0,
  var reads: Int = 0
)