package fast.runtime

import fast.inventory.Inventory
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object DeployFastDI {
  var FAST: Kodein = Kodein {}
    set(value) {
      FASTD = value.direct
      field = value
    }

  init {
    FAST = Kodein {
      bind<AppContext>() with singleton { AppContext() }

      bind("runAtHosts") from singleton {
        val runAt = FAST.direct.instance(tag = "runAt") as String
        val inventory = FAST.direct.instance<Inventory>()
        inventory.getHostsForName(runAt)
      }
    }
  }


  var FASTD = FAST.direct
}