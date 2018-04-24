package fast.runtime

import fast.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

class AllSessionsRuntimeContext(
  val inventory: Inventory
) {
  val sessions: ConcurrentHashMap<String, TaskContext> = ConcurrentHashMap()
}