package fast.dsl

import fast.runtime.AllSessionsRuntimeContext
import fast.runtime.TaskContext

data class TaskResult(
  val ok: Boolean = true,
  val modified: Boolean = true,
  val stdout: String = "",
  val stderr: String? = null,
  val code: Int = 0,
  val comment: String? = null
) {
  operator fun times(other: TaskResult): TaskResult {
    return TaskResult(
      ok && other.ok,
      modified || other.modified
    )
  }

  companion object {
    val ok = TaskResult()
  }
}

interface ITask {
  val name: String
  val desc: String?
  val extension: DeployFastExtension<ExtensionConfig>?

  suspend fun play(taskContext: TaskContext): TaskResult

  val before: TaskSet
  val after: TaskSet
  suspend fun doIt(context: TaskContext): TaskResult
}

open class Task(
  override val name: String,
  override val desc: String? = null,
  override val extension: DeployFastExtension<ExtensionConfig>? = null
) : ITask {
  override val before by lazy { TaskSet("before") }
  override val after by lazy { TaskSet("after") }

  /** Each task definition belongs to exactly one extension */



  override suspend final fun play(context: TaskContext): TaskResult {
    return context.play(this)
  }

  override suspend fun doIt(context: TaskContext): TaskResult {
    TODO("not implemented")
  }

  companion object {
    val root = LambdaTask("root", null, { TaskResult.ok })
  }
}

/**
 * Extension task creates it's own context which corresponds to extension function
 */
class ExtensionTask(name: String, desc: String? = null, extension: DeployFastExtension<ExtensionConfig>, block: suspend (TaskContext) -> TaskResult)
  : LambdaTask(name, desc, extension, block) {
  fun asTask(): Task {
    return LambdaTask(name, desc, extension, block)
  }
}

open class LambdaTask(name: String, desc: String? = null, extension: DeployFastExtension<ExtensionConfig>? = null, val block: suspend (TaskContext) -> TaskResult)
  : Task(name, desc, extension) {

  constructor(
    name: String,
    extension: DeployFastExtension<ExtensionConfig>?,
    block: suspend (TaskContext) -> TaskResult
  )
    : this(name, null, extension, block)

  override suspend fun doIt(context: TaskContext): TaskResult {
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

  override suspend fun doIt(context: TaskContext): TaskResult {
    var r = TaskResult.ok

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

open class NamedExtTasks(val extension: DeployFastExtension<ExtensionConfig>) {
//  lateinit var extension: DeployFastExtension<ExtensionConfig>
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
) {
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

  fun task(name: String = "", block: suspend TaskContext.() -> TaskResult): Unit {
    taskSet.append(LambdaTask(name, null, block))
  }

//  infix fun String.task(block: TaskContext.() -> TaskResult) = task(this, block)
}

inline fun <T> Any?.ifNotNull(block: () -> T): T {
  return block.invoke()
}

/**
 * TODO: rename ext into i.e. extensions,
 */
class DeployFastDSL<CONF : ExtensionConfig, EXT : DeployFastExtension<CONF>>(
  val ext: EXT
) {
  internal var info: InfoDSL? = null

  val tasks: TaskSet = TaskSet(ext.name, "Tasks of extension ${ext.name}", ext as DeployFastExtension<ExtensionConfig>)
  val globalTasks: TaskSet = TaskSet("${ext.name}.global", "Global Tasks for Extension $ext", ext as DeployFastExtension<ExtensionConfig>)

  fun autoInstall(): Unit = TODO()

  fun info(block: InfoDSL.() -> Unit): Unit {
    info = InfoDSL().apply(block)
  }

  private var beforeGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null
  private var afterGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null

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
    fun <CONF : ExtensionConfig, EXT : DeployFastExtension<CONF>> deployFast(
      ext: EXT, block: DeployFastDSL<CONF, EXT>.() -> Unit

    ): DeployFastDSL<CONF, EXT> {
      val deployFastDSL = DeployFastDSL(ext)

      deployFastDSL.apply(block)

      return deployFastDSL
    }
  }
}

fun main(args: Array<String>) {
}