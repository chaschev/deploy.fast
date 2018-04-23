package fast.runtime

import fast.dsl.DeployFastDSL
import fast.dsl.Task
import fast.dsl.TaskContext
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Job

class SessionRuntimeContext(
  val path: String,
  val task: Task,
  val ssh: SshProvider,
  val allSessionsContext: AllSessionsRuntimeContext,
  val parent: SessionRuntimeContext?
) {

  private val children = ArrayList<SessionRuntimeContext>()

  private val stats = TaskStats()

  @Volatile
  private var job: Job? = null

//  fun resolveVar(varName: String): String {
//    // check if my groups override
//    // check if my host overrides
//    // if not, check if name is ${extension}
//    TODO()
//  }

  fun init() {
    //todo override config values
  }

  /**
   * Api to play a Task
   */
  fun play(task: Task) {

    val newChildContext = SessionRuntimeContext(
      "$path.${task.name}",
      task,
      ssh,
      allSessionsContext,
      this
    )

    children += newChildContext

    job = asyncNoisy {
      //TODO: update result
      for (beforeTask in task.before.tasks()) {
        task.play(TaskContext(newChildContext))
      }

      task.play(TaskContext(newChildContext))

      for (afterTask in task.after.tasks()) {
        task.play(TaskContext(newChildContext))
      }
    }
  }

  fun play(dsl: DeployFastDSL<*>) {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    play(dsl.tasks)
  }
}

