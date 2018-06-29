package fast.sample

import fast.api.DeployFastApp
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFast
import fast.runtime.DeployFast.FAST
import fast.runtime.DeployFast.FASTD
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


object VpnAppDI {
  init {
    DeployFast.FAST = Kodein {
      extend(FAST)

      bind<Inventory>() with singleton {
        Inventory(
          listOf(
            Group(
              name = "vpn",
              hosts = listOf(
//                Host("vpn1")
                Host("vpn2")
              )
            ),
            Group(
              name = "vm",
              hosts = listOf(
                Host("192.168.5.10", "vm1") //,
//                Host("192.168.5.11", "vm2"),
//                Host("192.168.5.12", "vm3")
              )
            )
          )
        ).initHosts()
      }

      bind<DeployFastApp<*>>() with singleton { VpnFastApp() }

      bind("dsl") from singleton { VpnFastApp.dsl() }

      bind("runAt") from singleton {
        FASTD
          .instance<Inventory>()
          .group("vpn")
      }
//      bind("runAtHosts") from singleton {
//        val runAt = FASTD.instance(tag = "runAt") as String
//        val inventory = FASTD.instance<Inventory>()
//        inventory.asOneGroup.getHostsForName(runAt)
//      }


    }
  }

}

