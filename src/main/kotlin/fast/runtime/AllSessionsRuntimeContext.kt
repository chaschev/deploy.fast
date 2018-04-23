package fast.runtime

import fast.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

class AllSessionsRuntimeContext(
  val inventory: Inventory
) {
  val contexts: ConcurrentHashMap<String, SessionRuntimeContext> = ConcurrentHashMap()
}