package fast.runtime

import fast.inventory.Inventory
import org.kodein.di.generic.instance


class AppContext {
  val inventory: Inventory by DeployFast.FAST.instance()
  val hosts by lazy {inventory.activeHosts}

  fun addresses() = hosts.map { it.address }

  val globalMap = GlobalMap()

}