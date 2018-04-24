package fast.runtime

import fast.dsl.DeployFastDSL
import fast.dsl.ExtensionConfig
import fast.dsl.Task
import fast.dsl.TaskResult
import fast.dsl.TaskResult.Companion.ok
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Job


//User accesses Task Context
//TODO consider another abstraction level: separate user context from logic
class TaskContext(
  val global: AllSessionsRuntimeContext,
  val session: SessionRuntimeContext,
  val parent: TaskContext?
) {
  constructor(session: SessionRuntimeContext, parent: TaskContext) : this(parent.global, session, parent)

  val ssh = session.ssh
  lateinit var config: ExtensionConfig

  private val children = ArrayList<TaskContext>()

  // job is required for cancellation and coordination between tasks
  @Volatile
  private var job: Job? = null

  /**
   * Api to play a Task
   *
   * That's the only entry point for playing tasks!
   */
  private fun playOneTask(childTask: Task): TaskResult {
    val childSession = session.newChildContext(childTask)

    val childContext = TaskContext(childSession, this)

    // almost everything is initialised before execution
    // now, update vars extension's config
    // and then start
    children += childContext

    childContext.config = childTask.extension.config(childContext)

    return childTask.play(childContext)
  }

  fun play(childTask: Task): TaskResult {
    var result = ok

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

  fun play(dsl: DeployFastDSL<*, *>) {
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