package fast.dsl

import fast.runtime.AllSessionsRuntimeContext
import fast.runtime.SessionRuntimeContext

data class TaskResult(
  val ok: Boolean = true,
  val modified: Boolean = true,
  val stdout: String = "",
  val stderr: String? = null,
  val code: Int = 0,
  val comment: String? = null
) {

}

interface ITask {
  val name: String
  val desc: String?

  fun run() : TaskResult

  val before: TaskSet
  val after: TaskSet
}

open class Task(
  override val name: String,
  override val desc: String? = null
) : ITask {
  override val before by lazy { TaskSet() }
  override val after  by lazy { TaskSet() }

  internal lateinit var extension: DeployFastExtension

  open internal fun play(context: TaskContext) : TaskResult {
    TODO()
  }


  override fun run(): TaskResult {
    return run(null!!)
  }
}

open class LambdaTask(name: String, val block: (TaskContext) -> TaskResult): Task(name) {
  override fun play(context: TaskContext) : TaskResult {

    return block.invoke(context)
  }
}

// TODO: consider - can be a composite task
class TaskSet(name: String = "default", desc: String? = null) : Task(name, desc), Iterable<Task> {

  private val tasks = ArrayList<Task>()

  fun append(task: Task) = tasks.add(task)

  fun insertFirst(task: Task) = tasks.add(0, task)

  fun append(name: String = "", block: TaskContext.() -> TaskResult) {
    tasks.add(LambdaTask(name, block))
  }

  fun tasks(): List<Task> = tasks

  override fun play(context: TaskContext): TaskResult {
    //       //TODO: update result

    for (task in tasks) {
      context.session.play(task)
    }

    /*

    TODO: Running a composite task
     Run each task one after other in a normal way through session context

     After: combine results

    TODO: running a task
   thisNode.session = ..., context=... , startMs =
   add a named task to task tree, set tree.currentRunNode = thisNode
   run
    save & log output
    get task result, record time
    (lazy evaluation) apply task result to
     session's global result
     parent task result
*/
    TODO("combine results")
  }

  fun addAll(taskSet: TaskSet) {
    tasks.addAll(taskSet.tasks)
  }

  override fun iterator(): Iterator<Task> {
    return tasks.iterator()
  }

}

open class NamedExtTasks {
  lateinit var context: TaskContext

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

class SystemServiceAppExtension: DeployFastExtension() {
  override val tasks: NamedExtTasks
    get() = TODO()
}


class TaskContext(
  val session: SessionRuntimeContext
) {
  val ssh = session.ssh
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

  fun init(block: () -> Unit)= block.invoke()

  fun task(name:String = "", block: TaskContext.() -> TaskResult): Unit {
    taskSet.append(LambdaTask(name, block))
  }

//  infix fun String.task(block: TaskContext.() -> TaskResult) = task(this, block)
}

inline fun <T> Any?.ifNotNull(block: () -> T): T {
  return block.invoke()
}
/**
 * TODO: rename ext into i.e. extensions,
 */
class DeployFastDSL<EXT : DeployFastExtension>(val ext: EXT) {
  internal var info: InfoDSL? = null

  val tasks: TaskSet = TaskSet()
  val globalTasks: TaskSet = TaskSet()
//  var beforeTasks: TaskSet? = null
//  var afterTasks: TaskSet? = null

  fun autoInstall(): Unit = TODO()

  fun info(block: InfoDSL.() -> Unit): Unit {
    info = InfoDSL().apply(block)
  }

  private var beforeGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null
  private var afterGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null

  fun beforeGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    beforeGlobalTasks = block
  }

  fun afterGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    afterGlobalTasks = block
  }

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
    fun  <EXT : DeployFastExtension> deployFast(ext: EXT, block: DeployFastDSL<EXT>.() -> Unit): DeployFastDSL<EXT> {
      val deployFastDSL = DeployFastDSL<DeployFastExtension>(ext) as DeployFastDSL<EXT>

      deployFastDSL.apply(block)

      return deployFastDSL
    }
  }
}

fun main(args: Array<String>) {
}