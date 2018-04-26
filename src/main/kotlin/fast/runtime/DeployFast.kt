package fast.runtime

import fast.dsl.*
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.ssh.GenericSshProvider
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging


class AppContext(
  runAt: String,
  val global: AllSessionsRuntimeContext
) {
  val inventory = global.inventory

  val hosts: List<Host> = inventory.asOneGroup.getHostsForName(runAt)
}

class DeployFastMain {
  companion object : KLogging() {
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


      val scheduler = DeployFastScheduler({ CrawlersAppDeploy.dsl(it) }, "vm", inventory)

      runBlocking {
        scheduler.doIt()
      }
    }
  }
}

class DeployFastScheduler<APP : DeployFastApp>(
  dslLambda: (AppContext) -> DeployFastAppDSL<APP>,
  val runAt: String,
  val inventory: Inventory
) {
  val allSessionsContext = AllSessionsRuntimeContext(inventory)

  val app = AppContext(runAt, allSessionsContext)

  val dsl = dslLambda(app)

  init {
    require(inventory.initialised(), { "inventory has not been initialized with init()" })
  }


  suspend fun doIt() {
    playGlobalTasks()

    val jobs = startSessions()

    await(jobs)
  }

  private suspend fun await(jobs: List<Deferred<ITaskResult>>) {
    println("awaiting for ${jobs.size} to finish")

    jobs.forEachIndexed { index, job ->
      println("awaiting for job ${index + 1}...")
      val result = job.await()
      println("got result from a job ${index + 1}: $result")
    }
  }

  suspend fun playGlobalTasks() {
    val ctx = SessionRuntimeContext(
      dsl.globalTasks, null, "", allSessionsContext, Host("local"), SshProvider.dummy)

    val taskCtx = TaskContext(dsl.globalTasks, allSessionsContext, ctx, null)

    taskCtx.play(dsl.globalTasks)
  }

  suspend fun startSessions(): List<Deferred<ITaskResult>> {
    return app.hosts.map { host ->
      asyncNoisy {
        val ssh = connect(host)

        val rootTaskContext = createRootSessionContext(host, ssh)

        rootTaskContext.play(dsl)
      }
    }
  }

  private fun connect(host: Host): SshProvider {
    val sshConfig = dsl.ssh!!.forHost(host)
    val sshImpl = GenericSshProvider(sshConfig)

    return sshImpl.connect()
  }

  private fun createRootSessionContext(host: Host, ssh: SshProvider): TaskContext {
    val rootSessionContext = SessionRuntimeContext(
      Task.root, null, "", allSessionsContext, host, ssh)

    val rootTaskContext = TaskContext(dsl.tasks, allSessionsContext, rootSessionContext, null)

    allSessionsContext.sessions[host.address] = rootTaskContext
    return rootTaskContext
  }


}

/*
object DeployFast : KLogging() {

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
}*/
