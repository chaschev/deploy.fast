package fast.runtime

import fast.api.*
import fast.dsl.*
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFast.FASTD
import fast.ssh.GenericSshProvider
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import org.kodein.di.generic.instance

class DeployFastScheduler<APP : DeployFastApp<APP>> {
  val allSessionsContext = AllSessionsRuntimeContext()

  val appCtx: AppContext by DeployFast.FAST.instance()

  val dsl = DeployFast.FASTD.instance<Any>("dsl") as DeployFastAppDSL<APP>

  val inventory: Inventory by DeployFast.FAST.instance()

  init {
    require(inventory.initialised(), { "inventory has not been initialized with init()" })
  }

  suspend fun doIt() {
    playGlobalTasks()

    val jobs = startSessions()

    await(jobs)
  }

  private suspend fun await(jobs: List<Deferred<AnyAnyResult>>) {
    println("awaiting for ${jobs.size} to finish")

    jobs.forEachIndexed { index, job ->
      println("awaiting for job ${index + 1}...")
      val result = job.await()
      println("got result from a job ${index + 1}: $result")
    }
  }

  suspend fun playGlobalTasks() {
    val taskCtx = rootContext()

    taskCtx.play(dsl.globalTasks)
  }

  private fun rootContext(): TaskContext<Any, *, ExtensionConfig> {
    val ctx = SessionRuntimeContext(
      Task.root as AnyTaskExt<*>,
      null,
      allSessionsContext,
      Host("local"),
      SshProvider.dummy
    )

    val session = ctx
    val taskCtx = TaskContext(
      Task.root as LambdaTask<Any, Task.DummyApp, NoConfig>, //must be a bug in idea?!
      session,
      null
    )

    taskCtx.config = NoConfig()

    return taskCtx as TaskContext<Any, *, ExtensionConfig>
  }

  suspend fun startSessions(): List<Deferred<AnyAnyResult>> {
    val rootDeployApp = FASTD.instance<DeployFastApp<*>>() as APP

    return appCtx.hosts.map { host ->
      GlobalScope.asyncNoisy {
        val ssh = connect(host)

        val rootTaskContext = rootDeployApp.createRootSessionContext(host, ssh)

        rootTaskContext.play(dsl)
      }
    }.toList()
  }

  private fun connect(host: Host): SshProvider {
    val sshConfig = dsl.ssh!!.forHost(host)
    val sshImpl = GenericSshProvider(sshConfig)

    inventory.sshConfig = sshConfig

    return sshImpl.connect()
  }

  private fun <APP: DeployFastApp<APP>> DeployFastApp<APP>.createRootSessionContext(host: Host, ssh: SshProvider): TaskContext<*, *, *> {

    val rootDeployApp = FASTD.instance<DeployFastApp<*>>()

    val task = rootDeployApp.asTask

    val rootSessionContext = SessionRuntimeContext(
      task, null, allSessionsContext, host, ssh)

    val rootTaskContext = TaskContext<Any, APP, NoConfig>(task as Task<Any, APP, NoConfig>, rootSessionContext, null)

    allSessionsContext.sessions[host.address] = rootTaskContext

    return rootTaskContext
  }






}