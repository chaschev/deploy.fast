package fast.runtime

import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FASTD
import org.kodein.di.generic.instance


class AppContext {
  val inventory: Inventory by DeployFastDI.FAST.instance()
  val hosts by lazy {inventory.activeHosts}

  fun addresses() = hosts.map { it.address }

  val globalMap = GlobalMap()

}