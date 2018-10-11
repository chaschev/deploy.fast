package fast.runtime

import fast.inventory.Host
import fast.inventory.IGroup
import fast.inventory.Inventory
import fast.log.KLogging
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.net.NetworkInterface

inline fun <reified V : Any> Kodein.MainBuilder.bindFromEnv(
  tag: String,
  noinline transform: ((String?) -> V)? = null) {
  val value = System.getenv(tag) ?: System.getProperty(tag, null)

  val transformed = if (transform != null) {
    transform(value)
  } else {
    println("bindFromEnv (fast lib): default transform for tag=$tag, value $value")
    when (V::class) {
      String::class -> value
      Int::class -> value?.toInt()
      else -> value
    } as V
  }

  println("binding $tag to $transformed")

  bind(tag) from instance(transformed)
}

object DeployFast {
  var FAST: Kodein = Kodein {}
    set(value) {
      FASTD = value.direct
      field = value
    }

  init {
    FAST = Kodein {
      bind<AppContext>() with singleton { AppContext() }

      bind("activeGroup") from singleton {
        val runAt = FAST.direct.instance(tag = "runAt") as String
        val inventory = FAST.direct.instance<Inventory>()

        inventory.group(runAt)
      }

      bindFromEnv ("runAt") { it ?: "dev" }


      bind("runAtHosts") from singleton {
        //        val runAt = FAST.direct.instance(tag = "runAt") as String
//        val inventory = FAST.direct.instance<Inventory>()
//        inventory.getHostsForName(runAt)
        (FAST.direct.instance(tag = "runAt") as IGroup).hosts
      }

      bind<Host>() with singleton {
        val activeGroup = FAST.direct.instance("activeGroup") as IGroup
        var hostFound: Host? = null

        for (iface in NetworkInterface.getNetworkInterfaces().asSequence()) {
          for (address in iface.inetAddresses.asSequence()) {
//            logger.debug { "trying address: $address" }

//            val hostName = address.hostName
            val hostAddress = address.hostAddress

//            println("trying address: $hostAddress")

            val host: Host? = activeGroup.hosts.find { it.address == hostAddress }
/* that is a little slow
            ?:
            activeGroup.hosts.find {
              it.name == hostName
            }*/

            if (host != null) {
              hostFound = host
              break
            }
          }

          if(hostFound != null) break
        }

        hostFound ?: throw IllegalStateException("could not initialize current host")
      }

    }
  }


  var FASTD = FAST.direct
}