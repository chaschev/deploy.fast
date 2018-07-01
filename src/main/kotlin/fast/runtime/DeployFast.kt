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

inline fun <reified V : Any> Kodein.MainBuilder.bindFromEnv(tag: String, transform: ((String?) -> V) = { value ->
  when (V::class) {
    String::class -> value
    Int::class -> value?.toInt()
    else -> value
  } as V
}) {
  val value = System.getenv(tag) ?: System.getProperty(tag, null)
  println("binding $tag to $value")

  val transformed = transform(value)

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

      bindFromEnv<String>("runAt")

      bind<Host>() with singleton {
        val activeGroup = FAST.direct.instance("activeGroup") as IGroup

        for (iface in NetworkInterface.getNetworkInterfaces().asSequence()) {
          for (address in iface.inetAddresses.asSequence()) {
//            logger.debug { "trying address: $address" }

//            val hostName = address.hostName
            val hostAddress = address.hostAddress

//            println("trying address: $hostAddress")

            val host: Host? = activeGroup.hosts.find { it.address == hostAddress }
// that is a little slow
//            ?:
//            activeGroup.hosts.find {
//              it.name == hostName
//            }

            if (host != null) return@singleton host!!
          }
        }

        throw IllegalStateException("could not initialize current host")
      }

      bind("runAtHosts") from singleton {
        //        val runAt = FAST.direct.instance(tag = "runAt") as String
//        val inventory = FAST.direct.instance<Inventory>()
//        inventory.getHostsForName(runAt)
        (FAST.direct.instance(tag = "runAt") as IGroup).hosts
      }
    }
  }

  fun initFromEnv(vararg vars: String = arrayOf(
    "runAt", "one.cluster", "one.instance"
  )): DeployFast {
    val notNullVars = vars.mapNotNull { name ->
      val value = System.getenv(name) ?: System.getProperty(name)

      value?.let { name to it }
    }

    if (notNullVars.isNotEmpty()) {
      FAST = Kodein {
        extend(FAST, allowOverride = true)

        for ((k, v) in notNullVars) {
          println("binding $k to $v")
          bind(k) from instance(v)
        }
      }
    }

    return this
  }


  var FASTD = FAST.direct
}