package fast.runtime

import fast.dsl.DeployFastApp
import fast.dsl.DeployFastAppDSL
import fast.dsl.ITaskResult
import fast.dsl.Task
import fast.inventory.Host
import fast.inventory.Inventory
import fast.ssh.GenericSshProvider
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import org.kodein.di.generic.instance

class DeployFastScheduler<APP : DeployFastApp> {
  val allSessionsContext = AllSessionsRuntimeContext()

  val app: AppContext by DeployFastDI.FAST.instance()

  val dsl = DeployFastDI.FASTD.instance<Any>("dsl") as DeployFastAppDSL<APP>

  val inventory: Inventory by DeployFastDI.FAST.instance()

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