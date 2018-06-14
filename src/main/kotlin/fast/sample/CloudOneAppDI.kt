package fast.sample

import fast.api.DeployFastApp
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import fast.runtime.DeployFastDI.FASTD
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


object CloudOneAppDI {
  init {
    FAST = Kodein {
      extend(FAST)

      bind<Inventory>() with singleton {
        Inventory(
          listOf(
            Group(
              name = "dev",
              hosts = listOf(
                Host("localhost", "laptop")
              )
            ),
            Group(
              name = "vpn",
              hosts = listOf(
                Host("vpn1"),
                Host("vpn2")
              )
            ),
            Group(
              name = "vm",
              hosts = listOf(
                Host("192.168.5.10", "vm1"),
                Host("192.168.5.11", "vm2"),
                Host("192.168.5.12", "vm3")
              )
            )
          )
        ).init()
      }

      bind<DeployFastApp<*>>() with singleton { CloudOneFastApp() }

      bind("dsl") from singleton { CloudOneFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

}

