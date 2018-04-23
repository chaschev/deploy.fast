package fast.runtime

class TaskStats (
  var startMs: Long = 0,
  var endMs: Long = 0,
  var exitCode: Int = -1
) {
  val counters = TaskStatsCounters()
}