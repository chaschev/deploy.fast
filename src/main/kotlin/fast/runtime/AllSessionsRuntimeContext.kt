package fast.runtime

import java.util.concurrent.ConcurrentHashMap

class AllSessionsRuntimeContext {
  val sessions: ConcurrentHashMap<String, TaskContext> = ConcurrentHashMap()
}