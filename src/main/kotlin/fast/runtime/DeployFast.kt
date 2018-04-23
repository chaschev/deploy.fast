package fast.runtime

import fast.dsl.*
import fast.dsl.ext.OpenJdkExtension
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.ssh.GenericSshProvider
import fast.ssh.KnownHostsConfig
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.runBlocking


class CrawlersExtension() : DeployFastExtension() {
  val vagrant = VagrantExtension().configure {
    mem = 2048
  }
  val openJdk = OpenJdkExtension().configure {
    pack = "openjdk-8-jdk"
  }

  companion object {
    fun dsl() = DeployFastDSL.deployFast(CrawlersExtension()) {
      info {
        name = "Vagrant Extension"
        author = "Andrey Chaschev"
      }

      beforePlay {
        ext.vagrant
      }

      play {
        task {
          println("jdk installation status" + ext.openJdk.getStatus())

          TaskResult()
        }
      }
    }

  }
}



object DeployFast {

  fun runIt() {

  }

  @JvmStatic
  fun main(args: Array<String>) {
    val inventory = Inventory(
      listOf(
        Group(
          name = "vm",
          hosts = listOf(
            Host("192.168.5.10")
          )
        )
      )
    )

    runBlocking {

      val allSessionsContext = AllSessionsRuntimeContext(inventory)

      val dsl = CrawlersExtension.dsl()

      dsl.ext.init(allSessionsContext)

      arrayOf("vpn1")
        .map { Host(it) }
        .map { host ->
          asyncNoisy {
            //TODO provide hosts to vagrant plugin

            val sshImpl = GenericSshProvider(KnownHostsConfig(
              path = "${System.getenv("HOME")}/ssh/.known_hosts",
              address = host.address,
              authUser = "root")
            )

            val ssh = sshImpl.connect()

            val rootSessionContext = SessionRuntimeContext(
              "", dsl.tasks, ssh, allSessionsContext, null)

            allSessionsContext.contexts[host.address] = rootSessionContext

            rootSessionContext.play(dsl)
          }
        }.forEachIndexed { index, job ->
          println("awaiting for job ${index + 1}...")
          val ls = job.await()
          println("done awaiting for baby")
        }
    }

  }
}