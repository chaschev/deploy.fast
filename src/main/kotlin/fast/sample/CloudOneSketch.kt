package fast.sample

import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import fast.sample.CloudOneClusterDsl.Companion.cluster
import org.kodein.di.generic.instance
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/*
 * Subcluster X:
 *
 * Instance1  Instance2   Instance3
 *      U           U
 *                  F           F
 *      M           M
 *
 *  Each of the instance can respond to any of the calls
 *  The DB is shared
 *  Each instance has data of M service
 *  No M for Instance 3 means: rateLimit = 0 + it cannot handle local keys
 *  Does it make any sense?
 *  It is difficult to say that
 *
 *  U = User Service
 *  F = Feed Service
 *  M = Messaging Service
 *
 *  Which means that each instance of the cluster can
 *   inject U, F or M service
 *
 *  Managing CloudOne:
 *
 *   1) submit configuration change
 *   2) approve on the server or apply directly if it is not-interactive and no problems found
 *   3) what can be changed:
 *      up/down instances
 *      change hazelcast settings
 *      change rate limiting
 *       how to rate limit remote hazelcast call?
 *        "easy": if system is fine then direct call; else reply with a job
 *        "simple":
 *          while(true) { if(resourceManager.isMyTurn(this)) { doJob() } }
 *          todo: think how resource manager keeps track of system load and accepts new jobs
 *      run data update
 *      change rights
 *
 */


interface CloudOneService<I : CloudOneInstance> {
  val name: String
  val instance: I

  fun up()
  fun down()
}


open class CloudOneInstance(
  var host: Host = Host.local,
  var port: Int = 0
) {
  val services = arrayListOf<String>()

}

class CrawlersInstance(
  host: Host,
  port: Int
) : CloudOneInstance(
  host, port
) {
  val markets = arrayListOf<String>()
}

class CloudOneClusterDsl(name: String) {
  val subclusters = arrayListOf<CloudOneSubcluster<*>>()
  val inventory by FAST.instance<Inventory>()
  val hosts by lazy { inventory.getHostsForName(name) }

  inline fun subcluster(name: String, block: CloudOneSubcluster<CloudOneInstance>.() -> Unit) {
    CloudOneSubcluster(name, CloudOneInstance::class).apply(block)
  }

  inline fun <reified I : CloudOneInstance> subclusterCustom(name: String, block: CloudOneSubcluster<I>.() -> Unit) {
    CloudOneSubcluster(name, I::class).apply(block)
  }


  companion object {
    fun cluster(name: String, block: CloudOneClusterDsl.() -> Unit) {
      CloudOneClusterDsl(name).apply(block)
    }
  }
}

class CloudOneSubcluster<I : CloudOneInstance>(
  val name: String,
  val instanceClass: KClass<I>
) {
  val map = HashMap<Host, ArrayList<I>>()

  // block: (I.() -> Unit)? = null
  infix fun List<Host>.map(ports: IntRange): List<ArrayList<I>> {
    return flatMap { host -> ports.map { host to it } }
      .map { (host, port) ->
        val instances = map.getOrPut(host, { ArrayList() })

        val instance = instances.find { it.port == port }
          ?: instanceClass.createInstance()

        instance.host = host
        instance.port = port

        instances
      }
  }

  infix fun List<ArrayList<I>>.apply(block: I.() -> Unit) {
    forEach { it.apply { block } }
  }

  companion object {
    val deploymentMap = listOf(
      cluster("dev") {
        subcluster("main") {
          hosts map 7000..7001 apply {

          }
        }

        subcluster("webapp") {
          hosts map 9000..9001
        }

        subclusterCustom<CrawlersInstance>("crawlers") {
          hosts map 7002..7005 apply {
            markets += when (port) {
              7002 -> listOf("binance", "kucoin")
              7003 -> listOf("bitfinex")
              7004 -> listOf("bitstamp")
              else -> emptyList()
            }
          }
        }
      }

    )
  }
}


