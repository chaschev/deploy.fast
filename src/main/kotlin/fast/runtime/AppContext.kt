package fast.runtime

import fast.inventory.Host
import fast.inventory.Inventory
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.delay
import org.kodein.di.generic.instance
import java.util.concurrent.ConcurrentHashMap

class AppContext {
  val runAt: String by DeployFastDI.FAST.instance(tag = "runAt")

  val inventory: Inventory by DeployFastDI.FAST.instance()

  val hosts: List<Host> = inventory.asOneGroup.getHostsForName(runAt)
  val globalMap = ConcurrentHashMap<String, Any>()

  /**
   * Party coordination could be done in a similar fashion.
   *
   * I.e. can await for an
   *  (taskKey, AtomicInteger) value,
   *  or a certain state of parties
   *  shared result state isCompleted() = true. Parties report to this result
   */

  suspend fun awaitKey(path: String, timeoutMs: Long = 600_000): Boolean {
    val startMs = System.currentTimeMillis()

    while(true) {
      if(globalMap.containsKey(path)) return true

      if(System.currentTimeMillis() - startMs > timeoutMs) return false

      delay(50)
    }
  }

  suspend fun <R> runOnce(path: String, block: suspend () -> R): Deferred<R> {
    val r = globalMap.getOrElse(path, {
      asyncNoisy {
        block()
      }
    })

    return r as Deferred<R>
  }
}