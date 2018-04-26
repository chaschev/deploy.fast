package fast.runtime

import fast.dsl.*
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


class AppContext(
//  runAt: String,
//  val global: AllSessionsRuntimeContext,
//  override val dkodein: DKodein
) {
  val runAt: String by FAST.instance(tag = "runAt")

  val inventory: Inventory by FAST.instance()

  val hosts: List<Host> = inventory.asOneGroup.getHostsForName(runAt)
}

object DeployFastDI {
  var FAST = Kodein {
    bind<AppContext>() with singleton { AppContext() }
  }
    set(value) {
      FASTD = value.direct
      field = value
    }

  var FASTD = FAST.direct
}

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
                Host("192.168.5.10")
              )
            )
          )
        ).init()
      }

      bind("dsl") from singleton { CrawlersFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val scheduler = DeployFastScheduler<DeployFastApp>()

    runBlocking {
      scheduler.doIt()
    }
  }
}

