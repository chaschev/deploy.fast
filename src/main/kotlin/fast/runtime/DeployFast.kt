package fast.runtime

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.dsl.*
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import fast.ssh.logger
import fast.ssh.run
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.time.Duration
import java.time.Instant


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

class CrawlersFastApp : DeployFastApp<CrawlersFastApp>("crawlers") {
  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })

  val openJdk = OpenJdkExtension({
    OpenJdkConfig(
      pack = "openjdk-8-jdk"
    )
  })

  val cassandra = CassandraExtension({
    CassandraConfig(app.hosts)
  })

  companion object {
    fun dsl(): DeployFastAppDSL<CrawlersFastApp> {
      val app by FAST.instance<DeployFastApp<*>>()

      return DeployFastDSL.createAppDsl(app as CrawlersFastApp) {
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

          "other" with { privateKey(it) }
        }

        globalTasksBeforePlay {
          task("update_vagrantfile") {
            ext.vagrant.tasks(this).updateFile()
          }
        }

        play {
          task("check_java_and_speed_test") {
            println("jdk installation status:" + ext.openJdk.tasks(this).getStatus())

            val startedAt = Instant.now()
            val times = 3

            repeat(times) {
              val pwd = ssh.run("pwd; cd /etc; pwd")
              logger.debug { pwd.text()}
            }

            val duration = Duration.between(Instant.now(), startedAt)

            logger.info { "finished in $duration, which is ${duration.toMillis() / times}ms per operation" }

            TaskResult.ok
          }

          task("install_java") {
            ext.openJdk.tasks(this).installJava()
          }

          task("install_cassandra") {
            ext.cassandra.tasks(this).install()
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

      bind<DeployFastApp<*>>() with singleton { CrawlersFastApp() }

      bind("dsl") from singleton { CrawlersFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val scheduler = DeployFastScheduler<CrawlersFastApp>()

    runBlocking {
      scheduler.doIt()
    }
  }
}

