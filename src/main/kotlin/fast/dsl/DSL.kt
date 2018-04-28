package fast.dsl

import fast.inventory.Host
import fast.runtime.AllSessionsRuntimeContext
import fast.runtime.AnyTaskContext
import fast.runtime.DeployFastDI.FAST
import fast.runtime.DeployFastDI.FASTD
import fast.runtime.TaskContext
import fast.ssh.KnownHostsConfig
import fast.ssh.command.CommandResult
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.kodein.di.generic.instance


data class AggregatedValue(
  val list: ArrayList<Any> = ArrayList()
) {
  constructor(vararg items: Any) : this(items.toCollection(ArrayList()))
}


typealias AnyAnyResult = ITaskResult<*>
typealias AnyResult = ITaskResult<Any>
typealias BooleanResult = ITaskResult<Boolean>

interface ITaskResult<R> {
  val ok: Boolean
  val modified: Boolean
  val value: R

  operator fun times(other: ITaskResult<Any>): ITaskResult<Any> {
    return TaskResult(
      ok && other.ok,
      modified || other.modified,
      mergeValue(other)
    )
  }

  // merges into an aggregated value
  fun mergeValue(other: ITaskResult<Any>): Any {
    val v = value

    return if(v is AggregatedValue) {
      v.list.add(other.value)
      v
    } else {
      AggregatedValue(v as Any, other.value)
    }
  }



  fun <O> mapValue(block: (R) -> O) =
    TaskValueResult(block(value), this.ok, this.modified)
}

open class TaskResultAdapter<R>(
  override val ok: Boolean,
  override val modified: Boolean,
  override val value: R
) : ITaskResult<R> {

}

data class TaskResult<R>(
  override val ok: Boolean = true,
  override val modified: Boolean = true,
  override val value: R,
  val stdout: String = "",
  val stderr: String? = null,
  val code: Int = 0,
  val comment: String? = null
) : ITaskResult<R> {

  companion object {
    val ok = TaskResult(value = true)
  }
}

class TaskValueResult<T>(
  override val value: T,
  ok: Boolean = true,
  modified: Boolean = false) : TaskResultAdapter<T>(ok, modified, value) {

  override fun toString(): String {
    return """Result(ok=$ok, modified=$modified, value=$value)"""
  }
}

fun <T> CommandResult<T>.toFast(modified: Boolean = false): TaskValueResult<T> {
  return TaskValueResult(
    value, this.console.result.isOk(), modified
  )
}

interface ITask<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig> {
  val name: String
  val desc: String?
  val extension: EXT?

  suspend fun play(context: TaskContext<Any, EXT, EXT_CONF>) : ITaskResult<R>

  val before: TaskSet
  val after: TaskSet
  suspend fun doIt(context: AnyTaskContext): ITaskResult<R>
}

open class Task<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
  override val name: String,
  override val desc: String? = null,
  override val extension: EXT
) : ITask<R, EXT, EXT_CONF> {
  override val before by lazy { TaskSet("before") }
  override val after by lazy { TaskSet("after") }

  /** Each task definition belongs to exactly one extension */

  suspend final fun playChild(context: TaskContext<R, EXT, EXT_CONF>): ITaskResult<Any> {
    return context.play(this) as ITaskResult<Any>
  }

  override suspend final fun play(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R> {
    return context.play(this as Task<Any, EXT, EXT_CONF>) as ITaskResult<R>
  }

  override suspend fun doIt(context: AnyTaskContext): ITaskResult<R> {
    TODO("not implemented")
  }

  class DummyApp: DeployFastApp<DummyApp>("dummy")

  companion object {
    val rootExtension by FAST.instance<DeployFastApp<*>>()
    val dummyApp = DummyApp()
    val root = LambdaTask("root", dummyApp, { TaskResult.ok  as AnyResult })
    val dummy = LambdaTask("dummy", dummyApp, { TaskResult.ok  as AnyResult })
  }
}

/**
 * Extension task creates it's own context which corresponds to extension function
 */
class ExtensionTask<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
  name: String,
  extension: EXT,
  desc: String? = null,
  /* TODO: change this context to real task context */
  block: suspend AnyTaskContext.() -> ITaskResult<R>
) : LambdaTask<R, EXT, EXT_CONF>(name, desc, extension, block) {
  /*suspend final fun playExt(context: AnyTaskContext): ITaskResult<R> {
    return context.play(this) as ITaskResult<R>
  }*/

  fun asTask(): Task<R, EXT, EXT_CONF> {
    return LambdaTask(name, desc, extension, block)
  }
}

typealias AnyLambdaTask = LambdaTask<Any, *, ExtensionConfig>

open class LambdaTask<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
  name: String,
  desc: String? = null,
  extension: EXT,
  val block: suspend (TaskContext<Any, *, ExtensionConfig>) -> ITaskResult<R>
)
  : Task<R, EXT, EXT_CONF>(name, desc, extension) {

  constructor(
    name: String,
    extension: EXT,
    block: suspend (TaskContext<Any, *, ExtensionConfig>) -> ITaskResult<R>
  )
    : this(name, null, extension, block)

  override suspend fun doIt(context: AnyTaskContext): ITaskResult<R> {
    return block.invoke(context)
  }
}

typealias AnyTask = Task<Any, *, ExtensionConfig>
typealias AnyTaskExt<EXT> = Task<Any, EXT, ExtensionConfig>
typealias AnyExtensionTask<EXT> = ExtensionTask<Any, EXT, ExtensionConfig>

// TODO: consider - can be a composite task
class TaskSet(
  name: String = "default",
  desc: String? = null
) : Iterable<AnyTask> {

  private val tasks = ArrayList<AnyTask>()

  fun append(task: AnyTask) = tasks.add(task)

  fun insertFirst(task: AnyTask) = tasks.add(0, task)

  fun tasks(): List<AnyTask> = tasks

  fun addAll(taskSet: TaskSet) {
    tasks.addAll(taskSet.tasks)
  }

  override fun iterator(): Iterator<AnyTask> {
    return tasks.iterator()
  }

  fun size() = tasks.size

}

open class NamedExtTasks<EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
  val extension: EXT,
  parentCtx: AnyTaskContext
) {
  val extCtx: ChildTaskContext<EXT, EXT_CONF>
  //  lateinit var extension: DeployFastExtension<ExtensionConfig>

  init {
    extCtx = parentCtx.newChildContext(extension.asTask, extension.name) as ChildTaskContext<EXT, EXT_CONF>
  }

  open suspend fun getStatus(): ITaskResult<ServiceStatus> = TODO()
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
)  {
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

class TasksDSL<EXT: DeployFastExtension<EXT, CONF>, CONF: ExtensionConfig> {
  //  private val tasks = ArrayList<Task>()
  internal val taskSet = TaskSet()

  val app = FASTD.instance<DeployFastApp<*>>() as EXT

  fun init(block: () -> Unit) = block.invoke()

  fun task(
    name: String = "",
    block: suspend AnyTaskContext.() -> AnyAnyResult
  ): Unit {
    taskSet.append(LambdaTask<Any, EXT, CONF>(name, app  , block as suspend (TaskContext<Any, *, ExtensionConfig>) -> ITaskResult<Any>) as AnyTask)
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



class DeployFastAppDSL<APP: DeployFastApp<APP>>(ext: APP)
  : DeployFastDSL<NoConfig, APP>(ext) {

}

/**
 * TODO: rename ext into i.e. extensions,
 */
open class DeployFastDSL<CONF : ExtensionConfig, EXT : DeployFastExtension<EXT, CONF>>(
  val ext: EXT
) {
  internal var info: InfoDSL? = null
  internal var ssh: SshDSL? = null

  val tasks: TaskSet = TaskSet(
    name = ext.name,
    desc = "Tasks of extension ${ext.name}")

  val globalTasks: TaskSet = TaskSet("${ext.name}.global",
    "Global Tasks for Extension $ext"
  )

  fun autoInstall(): Unit = TODO()

  fun info(block: InfoDSL.() -> Unit) {
    info = InfoDSL().apply(block)
  }

  private var beforeGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null
  private var afterGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null

  fun ssh(block: SshDSL.() -> Unit) {
    ssh = SshDSL().apply(block)
  }

  fun globalTasksBeforePlay(block: TasksDSL<EXT, CONF>.() -> Unit) {
    globalTasks.addAll(TasksDSL<EXT, CONF>().apply(block).taskSet)
  }


  /*fun beforeGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    beforeGlobalTasks = block
  }

  fun afterGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    afterGlobalTasks = block
  }*/

  fun play(block: TasksDSL<EXT, CONF>.() -> Unit) {
    tasks.addAll(TasksDSL<EXT, CONF>().apply(block).taskSet)
  }

//  fun beforePlay(block: TasksDSL.() -> Unit) {
//    tasks.before.addAll(TasksDSL().apply(block).taskSet)
//  }
//
//  fun afterPlay(block: TasksDSL.() -> Unit) {
//    tasks.after.addAll(TasksDSL().apply(block).taskSet)
//  }


  companion object {
    fun <CONF : ExtensionConfig, EXT : DeployFastExtension<EXT, CONF>> createExtDsl(
      ext: EXT, block: DeployFastDSL<CONF, EXT>.() -> Unit

    ): DeployFastDSL<CONF, EXT> {
      val deployFastDSL = DeployFastDSL(ext)

      deployFastDSL.apply(block)

      return deployFastDSL
    }

    fun <APP : DeployFastApp<APP>> createAppDsl(
      app: APP, block: DeployFastAppDSL<APP>.() -> Unit

    ): DeployFastAppDSL<APP> {
      val deployFastDSL = DeployFastAppDSL(app)

      deployFastDSL.apply(block)

      return deployFastDSL
    }
  }
}
