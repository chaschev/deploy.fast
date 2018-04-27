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
  TaskContext<Any, AnyExtension<ExtensionConfig>, ExtensionConfig>

//User accesses Task Context
//TODO consider another abstraction level: separate user context from logic
class TaskContext<R, EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>
(
  val task: Task<R, EXT, EXT_CONF>,
  val session: SessionRuntimeContext,
  val parent: AnyTaskContext?
) {
  val ssh = session.ssh

  val global: AllSessionsRuntimeContext by FAST.instance()

  lateinit var config: EXT_CONF
  lateinit var extension: EXT

  private val children = ArrayList<TaskContext<R, EXT, EXT_CONF>>()

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
  internal fun newChildContext(childTask: AnyTask, customName: String? = null): AnyTaskContext {
    val childSession = session.newChildContext(childTask)

    val childContext = AnyTaskContext(
      childTask, childSession, this@TaskContext as AnyTaskContext
    )

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
        taskResult = playChildTask(before)
        result *= taskResult!!
      }

      taskResult = playOneTask(this)

      result *= taskResult!!

      if (after.size() > 0) {
        result *= playChildTask(after)
      }
    }

    return if (result.ok == taskResult?.ok) taskResult!! else result
  }

  suspend fun play(dsl: DeployFastDSL<*, *>): ITaskResult<Any> {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    return playChildTask(dsl.tasks)
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