package fast.runtime

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job


//User accesses Task Context
//TODO consider another abstraction level: separate user context from logic
class TaskContext
(
  val task: Task,
  val global: AllSessionsRuntimeContext,
  val session: SessionRuntimeContext,
  val parent: TaskContext?
) {
  constructor(task: Task, session: SessionRuntimeContext, parent: TaskContext) : this(task, parent.global, session, parent)

  val ssh = session.ssh

  lateinit var config: ExtensionConfig
  lateinit var extension: DeployFastExtension<ExtensionConfig>

  private val children = ArrayList<TaskContext>()

  // job is required for cancellation and coordination between tasks
  @Volatile
  private var job: Deferred<ITaskResult>? = null

  /**
   * Api to play a Task
   *
   * That's the only entry point for playing tasks!
   */
  private suspend fun playOneTask(childTask: Task): ITaskResult {
    job = asyncNoisy {
      val childContext = newChildContext(childTask)
      val doIt = childTask.doIt(childContext)
      doIt
    }

    return job!!.await()
  }

  internal fun newChildContext(childTask: Task): TaskContext {
    val childSession = session.newChildContext(childTask)

    val childContext = TaskContext(childTask, childSession, this@TaskContext)

    children += childContext

    // almost everything is initialised before execution
    // now, update vars extension's config
    // and then start


    if(childTask.extension != null) {
      childContext.config = childTask.extension!!.config(childContext)
      childContext.extension = childTask.extension!!
    } else {
      childContext.config = config
    }

    // TODO apply interceptors here (to change config variables)

    return childContext
  }

  suspend fun playExtensionTask(task: ExtensionTask): ITaskResult {
    val childCtx = newChildContext(task)
    return childCtx.play(task.asTask())
  }

  suspend fun play(childTask: Task): ITaskResult {
    // TODO apply interceptors here (modify tasks)
    if(childTask is ExtensionTask) return playExtensionTask(childTask)

    var result: ITaskResult = ok

    with(childTask) {
      if (before.size() > 0) {
        result *= play(before)
      }

      result *= playOneTask(this)

      if (after.size() > 0) {
        result *= play(after)
      }
    }

    return result
  }

  suspend fun play(dsl: DeployFastDSL<*, *>) {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    play(dsl.tasks)
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