package fast.runtime

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.runtime.DeployFastDI.FAST
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import mu.KLogging
import org.kodein.di.generic.instance
import java.security.cert.Extension


typealias AnyTaskContext =
  TaskContext<Any, *, ExtensionConfig>

typealias AnyTaskContextExt<EXT> =
  TaskContext<*, EXT, *>

typealias AnyAnyTaskContext =
  TaskContext<*, *, *>

//User accesses Task Context
//TODO consider another abstraction level: separate user context from logic
class TaskContext<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>
(
  val task: Task<R, EXT, EXT_CONF>,
  val session: SessionRuntimeContext,
  val parent: AnyAnyTaskContext?
) {
  val ssh = session.ssh

  val global: AllSessionsRuntimeContext by FAST.instance()

  lateinit var config: EXT_CONF
  lateinit var extension: EXT

  private val children = ArrayList<TaskContext<Any, EXT, EXT_CONF>>()

  // job is required for cancellation and coordination between tasks
  @Volatile
  private var job: Deferred<ITaskResult<Any>>? = null

  /**
   * Api to play a Task
   *
   * That's the only entry point for playing tasks!
   */
  private suspend fun playOneTask(childTask: AnyTask): ITaskResult<Any> {
    job = asyncNoisy {
      val childContext = newChildContext(childTask)

      logger.info { "playing task: ${childContext.session.path}" }

      val doIt = childTask.doIt(childContext as AnyTaskContext)
      doIt
    }

    return job!!.await()
  }

  /**
   * custom names are for new extension context, i.e.
   * task1.task2.apt::listPackages
   *             ^-- that is the custom name
   */
  internal fun newChildContext(childTask: Task<*, *, *>, customName: String? = null): TaskContext<Any, EXT, EXT_CONF> {
    val childSession = session.newChildContext(childTask)

    // we only need to create it
    // TODO switch to fucking reflection

    val childContext = TaskContext<Any, EXT, EXT_CONF>(
      childTask as Task<Any, EXT, EXT_CONF>, childSession, this
    )

    if(customName != null)
      childContext.session.path = "${session.path}.$customName"

    this.children.add(childContext)

    // almost everything is initialised before execution
    // now, update vars extension's config
    // and then start


    if(childTask.extension != null) {
      val childTask1 = childTask
      val extension1 = childTask1.extension
      childContext.config = extension1!!.config(childContext)
      childContext.extension = childTask.extension!!
    } else {
      println("no extension: " + childContext.session.path)
      childContext.config = config
    }

    // TODO apply interceptors here (to change config variables)

    return childContext
  }

  /*
  EXTENSION PLAY CHANGE!
  suspend fun playExtensionTask(task: AnyExtensionTask): ITaskResult<Any> {
    val childCtx = newChildContext(task, task.extension!!.name)
    return childCtx.play(task.asTask())
  }*/

  suspend fun play(task: Task<R, EXT, EXT_CONF>): ITaskResult<R> {
    return playChildTask(task as AnyTask) as ITaskResult<R>
  }

  suspend fun playChildTask(childTask: AnyTask): ITaskResult<Any> {
    // TODO apply interceptors here (modify tasks)
//    if(childTask is ExtensionTask) return playExtensionTask(childTask)

    var result: ITaskResult<Any> = ok as ITaskResult<Any>
    var taskResult: ITaskResult<Any>? = null

    with(childTask) {
      if (before.size() > 0) {
        taskResult = play(before)
        result *= taskResult!!
      }

      taskResult = playOneTask(this)

      result *= taskResult!!

      if (after.size() > 0) {
        result *= play(after)
      }
    }

    return if (result.ok == taskResult?.ok) taskResult!! else result
  }

  suspend fun play(dsl: DeployFastDSL<*, *>): ITaskResult<Any> {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    return play(dsl.tasks)
  }

  suspend fun play(tasks: Iterable<AnyTask>): AnyResult {
    var r: AnyResult = TaskResult.ok as AnyResult

    for (task in tasks) {
      r *= playChildTask(task)
    }

    return r
  }

  companion object: KLogging() {

  }

/*
  internal fun runChildTasks(childContext: SessionRuntimeContext) {
    job = asyncNoisy {
      //TODO: update result
      with(childContext) {
        for (beforeTask in childContext.task.before.tasks()) {
          task.play(childContext.newTaskCtx())
        }

        task.play(childContext.newTaskCtx())

        for (afterTask in task.after.tasks()) {
          task.play(childContext.newTaskCtx())
        }
      }
    }
  }
*/

}