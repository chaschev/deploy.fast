package fast.runtime

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.runtime.DeployFastDI.FAST
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import mu.KLogging
import org.kodein.di.generic.instance


//User accesses Task Context
//TODO consider another abstraction level: separate user context from logic
class TaskContext
(
  val task: Task,
  val session: SessionRuntimeContext,
  val parent: TaskContext?
) {
  val ssh = session.ssh

  val global: AllSessionsRuntimeContext by FAST.instance()

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

      logger.info { "playing task: ${childContext.session.path}" }

      val doIt = childTask.doIt(childContext)
      doIt
    }

    return job!!.await()
  }

  /**
   * custom names are for new extension context, i.e.
   * task1.task2.apt::listPackages
   *             ^-- that is the custom name
   */
  internal fun newChildContext(childTask: Task, customName: String? = null): TaskContext {
    val childSession = session.newChildContext(childTask)

    val childContext = TaskContext(childTask, childSession, this@TaskContext)

    if(customName != null)
      childContext.session.path = "${session.path}.$customName"

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
    val childCtx = newChildContext(task, task.extension!!.name)
    return childCtx.play(task.asTask())
  }

  suspend fun play(childTask: Task): ITaskResult {
    // TODO apply interceptors here (modify tasks)
    if(childTask is ExtensionTask) return playExtensionTask(childTask)

    var result: ITaskResult = ok
    var taskResult: ITaskResult? = null

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

  suspend fun play(dsl: DeployFastDSL<*, *>): ITaskResult {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    return play(dsl.tasks)
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