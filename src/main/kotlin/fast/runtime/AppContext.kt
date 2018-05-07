package fast.runtime

import fast.inventory.Host
import fast.inventory.Inventory
import org.kodein.di.generic.instance


class AppContext {
  val runAt: String by DeployFastDI.FAST.instance(tag = "runAt")

  val inventory: Inventory by DeployFastDI.FAST.instance()

  val hosts: List<Host> = inventory.asOneGroup.getHostsForName(runAt)

  fun addresses() = hosts.map { it.address }

  val globalMap = GlobalMap()

}