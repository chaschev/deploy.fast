package fast.dsl

import fast.inventory.Host
import fast.runtime.AllSessionsRuntimeContext
import fast.runtime.TaskContext
import fast.ssh.KnownHostsConfig
import fast.ssh.command.CommandResult

interface ITaskResult {
  val ok: Boolean
  val modified: Boolean

  operator fun times(other: ITaskResult): ITaskResult {
    return TaskResult(
      ok && other.ok,
      modified || other.modified
    )
  }
}

open class TaskResultAdapter(
  override val ok: Boolean,
  override val modified: Boolean
) : ITaskResult {

}

data class TaskResult(
  override val ok: Boolean = true,
  override val modified: Boolean = true,
  val stdout: String = "",
  val stderr: String? = null,
  val code: Int = 0,
  val comment: String? = null
) : ITaskResult {

  companion object {
    val ok = TaskResult()
  }
}

class TaskValueResult<T>(
  val value: T,
  ok: Boolean = true,
  modified: Boolean = false) : TaskResultAdapter(ok, modified) {

  override fun toString(): String {
    return """Result(ok=$ok, modified=$modified, value=$value)"""
  }

  inline fun <R> mapValue(block: (T) -> R) =
    TaskValueResult(block(value), this.ok, this.modified)
}

fun <T> CommandResult<T>.toFast(modified: Boolean = false): TaskValueResult<T?> {
  return TaskValueResult(
    value, this.console.result!!.isOk(), modified
  )
}

interface ITask {
  val name: String
  val desc: String?
  val extension: DeployFastExtension<ExtensionConfig>?

  suspend fun play(taskContext: TaskContext): ITaskResult

  val before: TaskSet
  val after: TaskSet
  suspend fun doIt(context: TaskContext): ITaskResult
}

open class Task(
  override val name: String,
  override val desc: String? = null,
  override val extension: DeployFastExtension<ExtensionConfig>? = null
) : ITask {
  override val before by lazy { TaskSet("before") }
  override val after by lazy { TaskSet("after") }

  /** Each task definition belongs to exactly one extension */


  override suspend final fun play(context: TaskContext): ITaskResult {
    return context.play(this)
  }

  override suspend fun doIt(context: TaskContext): ITaskResult {
    TODO("not implemented")
  }

  companion object {
    val root = LambdaTask("root", null, { TaskResult.ok })
  }
}

/**
 * Extension task creates it's own context which corresponds to extension function
 */
class ExtensionTask(
  name: String,
  extension: DeployFastExtension<ExtensionConfig>,
  desc: String? = null,
  block: suspend TaskContext.() -> ITaskResult
)
  : LambdaTask(name, desc, extension, block) {
  fun asTask(): Task {
    return LambdaTask(name, desc, extension, block)
  }
}

open class LambdaTask(name: String, desc: String? = null, extension: DeployFastExtension<ExtensionConfig>? = null, val block: suspend (TaskContext) -> ITaskResult)
  : Task(name, desc, extension) {

  constructor(
    name: String,
    extension: DeployFastExtension<ExtensionConfig>?,
    block: suspend (TaskContext) -> ITaskResult
  )
    : this(name, null, extension, block)

  override suspend fun doIt(context: TaskContext): ITaskResult {
    return block.invoke(context)
  }
}

// TODO: consider - can be a composite task
class TaskSet(
  name: String = "default",
  desc: String? = null,
  extension: DeployFastExtension<ExtensionConfig>? = null
) : Task(name, desc, extension), Iterable<Task> {

  private val tasks = ArrayList<Task>()

  fun append(task: Task) = tasks.add(task)

  fun insertFirst(task: Task) = tasks.add(0, task)

  fun tasks(): List<Task> = tasks

  override suspend fun doIt(context: TaskContext): ITaskResult {
    var r: ITaskResult = TaskResult.ok

    for (task in tasks) {
      r *= task.play(context)
    }

    return r
  }

  fun addAll(taskSet: TaskSet) {
    tasks.addAll(taskSet.tasks)
  }

  override fun iterator(): Iterator<Task> {
    return tasks.iterator()
  }

  fun size() = tasks.size

}

open class NamedExtTasks(
  val extension: DeployFastExtension<ExtensionConfig>,
  val taskCtx: TaskContext
) {
  //  lateinit var extension: DeployFastExtension<ExtensionConfig>
  open suspend fun getStatus(): ServiceStatus = TODO()

}

enum class RunningStatus {
  notStarted, started, running, stopped, aborted, unknown, notApplicable
}

enum class InstalledStatus {
  installed, notInstalled, installedWrongVersion, uninstalled, unknown
}

data class ServiceStatus(
  val installation: InstalledStatus,
  val running: RunningStatus = RunningStatus.notApplicable,
  val pid: Int? = null
) : TaskResultAdapter(true, false) {
  companion object {
    val installed = ServiceStatus(InstalledStatus.installed)
    val notInstalled = ServiceStatus(InstalledStatus.notInstalled)
  }
}

class InfoDSL(
) {
  var name: String = ""
  var author: String = ""
  var description: String = ""
}

class TasksDSL {
  //  private val tasks = ArrayList<Task>()
  internal val taskSet = TaskSet()

  fun init(block: () -> Unit) = block.invoke()

  fun task(name: String = "", block: suspend TaskContext.() -> ITaskResult): Unit {
    taskSet.append(LambdaTask(name, null, block))
  }

//  infix fun String.task(block: TaskContext.() -> TaskResult) = task(this, block)
}

inline fun <T> Any?.ifNotNull(block: () -> T): T {
  return block.invoke()
}

class SshDSL {
  val configs = LinkedHashMap<String, (Host) -> KnownHostsConfig>()

  infix fun String.with(block: (Host) -> KnownHostsConfig) {
    configs[this] = block
  }

  fun privateKey(host: Host, user: String = "root", block: (KnownHostsConfig.() -> Unit)? = null): KnownHostsConfig {
    val config = KnownHostsConfig(address = host.address, authUser = user)

    return (if (block != null) config.apply(block) else config)
  }

  fun password(
    host: Host, user: String, password: String,
    block: (KnownHostsConfig.() -> Unit)? = null
  ): KnownHostsConfig {
    val config = KnownHostsConfig(address = host.address, authUser = user, authPassword = password)

    return (if (block != null) config.apply(block) else config)
  }

  fun forHost(host: Host): KnownHostsConfig {
    /*for (group in host.groups) {
      val myConfig  = configs[group.name]
    }*/

    val configLambda = {
      // match by a group name
      val myGroup = host.groups.find { configs[it.name] != null }

      when {
        myGroup != null -> configs[myGroup.name]!!

      // match by a host name
        configs[host.name] != null -> configs[host.name]!!

      // check 'other'
        else -> configs["other"]
          ?: throw Exception("none of the hosts matched ssh configuration and 'other' ssh group is missing")
      }
    }()

    return configLambda(host)
  }


}


class DeployFastAppDSL<APP : DeployFastApp>(ext: APP)
  : DeployFastDSL<NoConfig, APP>(ext) {

}

/**
 * TODO: rename ext into i.e. extensions,
 */
open class DeployFastDSL<CONF : ExtensionConfig, EXT : DeployFastExtension<CONF>>(
  val ext: EXT
) {
  internal var info: InfoDSL? = null
  internal var ssh: SshDSL? = null

  val tasks: TaskSet = TaskSet(ext.name, "Tasks of extension ${ext.name}", ext as DeployFastExtension<ExtensionConfig>)
  val globalTasks: TaskSet = TaskSet("${ext.name}.global", "Global Tasks for Extension $ext", ext as DeployFastExtension<ExtensionConfig>)

  fun autoInstall(): Unit = TODO()

  fun info(block: InfoDSL.() -> Unit): Unit {
    info = InfoDSL().apply(block)
  }

  private var beforeGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null
  private var afterGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null

  fun ssh(block: SshDSL.() -> Unit) {
    ssh = SshDSL().apply(block)
  }

  fun globalTasksBeforePlay(block: TasksDSL.() -> Unit) {
    globalTasks.addAll(TasksDSL().apply(block).taskSet)
  }


  /*fun beforeGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    beforeGlobalTasks = block
  }

  fun afterGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    afterGlobalTasks = block
  }*/

  fun play(block: TasksDSL.() -> Unit) {
    tasks.addAll(TasksDSL().apply(block).taskSet)
  }

  fun beforePlay(block: TasksDSL.() -> Unit) {
    tasks.before.addAll(TasksDSL().apply(block).taskSet)
  }

  fun afterPlay(block: TasksDSL.() -> Unit) {
    tasks.after.addAll(TasksDSL().apply(block).taskSet)
  }


  companion object {
    fun <CONF : ExtensionConfig, EXT : DeployFastExtension<CONF>> createExtDsl(
      ext: EXT, block: DeployFastDSL<CONF, EXT>.() -> Unit

    ): DeployFastDSL<CONF, EXT> {
      val deployFastDSL = DeployFastDSL(ext)

      deployFastDSL.apply(block)

      return deployFastDSL
    }

    fun <APP : DeployFastApp> createAppDsl(
      app: APP, block: DeployFastAppDSL<APP>.() -> Unit

    ): DeployFastAppDSL<APP> {
      val deployFastDSL = DeployFastAppDSL(app)

      deployFastDSL.apply(block)

      return deployFastDSL
    }
  }
}

fun main(args: Array<String>) {
}