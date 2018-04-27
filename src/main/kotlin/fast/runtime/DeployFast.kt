package fast.runtime

import fast.dsl.*
import fast.dsl.ext.OpenJdkConfig
import fast.dsl.ext.OpenJdkExtension
import fast.dsl.ext.VagrantConfig
import fast.dsl.ext.VagrantExtension
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


class AppContext {
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

class CrawlersFastApp : DeployFastApp("crawlers") {

  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })

  val openJdk = OpenJdkExtension({
    OpenJdkConfig(
      pack = "openjdk-8-jdk"
    )
  })

  companion object {
    fun dsl(): DeployFastAppDSL<CrawlersFastApp> {
      return DeployFastDSL.createAppDsl(CrawlersFastApp()) {
        info {
          name = "Vagrant Extension"
          author = "Andrey Chaschev"
        }

        ssh {
          "vm" with {
            privateKey(it, "vagrant") {
              keyPath = "${"HOME".env()}/.vagrant.d/insecure_private_key"
            }
          }

          "other" with { privateKey(it)  }
        }

        globalTasksBeforePlay {
          task("update_vagrantfile") {
            ext.vagrant.tasks(this).updateFile().play(this)
          }
        }

        play {
          task("check_java") {
            println("jdk installation status:" + ext.openJdk.tasks(this).getStatus())

            TaskResult.ok
          }

          task("install_java") {
            ext.openJdk.tasks(this).installJava()
          }
        }
      }
    }
  }
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

