package fast.runtime

import fast.api.DeployFastApp
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton


object CrawlersAppDI {
  init {
    DeployFastDI.FAST = Kodein {
      extend(FAST)

      bind<Inventory>() with singleton {
        Inventory(
          listOf(
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

      bind<DeployFastApp<*>>() with singleton { CrawlersFastApp() }

      bind("dsl") from singleton { CrawlersFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

}

