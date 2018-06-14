package fast.sample

import fast.api.DeployFastApp
import fast.inventory.*
import fast.runtime.DeployFastDI.FAST
import fast.sample.DeployEnvironment.*
import fast.inventory.InventoryDsl.Companion.inventory
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

enum class DeployEnvironment {
  dev, vm, test, staging, prod
}

object CloudOneAppDI {
  init {
    FAST = Kodein {
      extend(FAST)

      bind<Inventory>() with singleton {
        inventory {
          group(dev) {
            hosts(Host("localhost", "laptop"))
          }

          group("vpn") {
            hosts(
              Host("vpn1"),
              Host("vpn2")
            )

            vars(
              "a" to "b"
            )

            subgroup(vm.name)
          }

          withGroups("vm", "vpn") {
            vars(
              "a" to "c",
              "d" to "e"
            )
          }

          group(vm) {
            hosts(
              Host("192.168.5.10", "vm1"),
              Host("192.168.5.11", "vm2"),
              Host("192.168.5.12", "vm3")
            )
          }
        }
      }

      bind<DeployFastApp<*>>() with singleton { CloudOneFastApp() }

      bind("dsl") from singleton { CloudOneFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

}

