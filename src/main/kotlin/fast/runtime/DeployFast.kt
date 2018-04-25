package fast.runtime

import fast.dsl.Task
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.ssh.GenericSshProvider
import fast.ssh.KnownHostsConfig
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging


class AppContext(
  val inventory: Inventory,
  val globalRuntime: AllSessionsRuntimeContext
) {
  lateinit var hosts: List<Host>
}

object DeployFast : KLogging() {
  fun runIt() {

  }

  @JvmStatic
  fun main(args: Array<String>) {
    logger.warn { "warn" }
    logger.info { "info" }
    logger.debug { "debug" }

    val inventory = Inventory(
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
    )

    inventory.init()

    val runAt = "vm"

    runBlocking {
      val allSessionsContext = AllSessionsRuntimeContext(inventory)
      val app = AppContext(inventory, allSessionsContext)

      app.hosts = inventory.asOneGroup.getHostsForName(runAt)

      val dsl = CrawlersAppDeploy.dsl(app)

      dsl.ext.init(allSessionsContext)

      val ctx = SessionRuntimeContext(
        dsl.globalTasks, null, "", allSessionsContext, Host("local"), SshProvider.dummy)

      val taskCtx = TaskContext(dsl.globalTasks, allSessionsContext, ctx, null)

      taskCtx.play(dsl.globalTasks)

      // START SESSIONS
      app.hosts.map { host ->
          asyncNoisy {
            //TODO provide hosts to vagrant plugin

            val sshConfig = dsl.ssh!!.forHost(host)
            val sshImpl = GenericSshProvider(sshConfig)

            val ssh = sshImpl.connect()

            val x = ssh.runSimple().ls("/")

            println(x)

            println(ssh.runSimple().pwd())

            val rootSessionContext = SessionRuntimeContext(
              Task.root, null, "", allSessionsContext, host, ssh)

            val rootTaskContext = TaskContext(dsl.tasks, allSessionsContext, rootSessionContext, null)

            allSessionsContext.sessions[host.address] = rootTaskContext

            rootTaskContext.play(dsl)
          }
        }.forEachIndexed { index, job ->
          println("awaiting for job ${index + 1}...")
          val ls = job.await()
          println("done awaiting for baby")
        }
    }

  }
}