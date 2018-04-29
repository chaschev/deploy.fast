package fast.runtime

import fast.api.Task
import fast.inventory.Host
import fast.ssh.SshProvider

/** TaskContext is 1-to-1 with SessionRuntimeContext, so generics can be copied */
class SessionRuntimeContext(
  val task: Task<*, *, *>,
  val parent: SessionRuntimeContext?,
  var path: String,
  val allSessionsContext: AllSessionsRuntimeContext,
  val host: Host,
  val ssh: SshProvider
) {
  constructor(task: Task<*, *, *>, parent: SessionRuntimeContext) :
    this(
      task = task,
      parent = parent,
      path = if (!parent.path.isBlank()) "${parent.path}.${task.name}" else task.name,
      allSessionsContext = parent.allSessionsContext,
      ssh = parent.ssh,
      host = parent.host
    )

  private val stats: TaskStats = TaskStats()

//  fun resolveVar(varName: String): String {
//    // check if my groups override
//    // check if my host overrides
//    // if not, check if name is ${extension}
//    TODO()
//  }

  fun init() {
    //todo override config values
  }

  internal fun newChildContext(task: Task<*, *, *>): SessionRuntimeContext {
    val newChildContext = SessionRuntimeContext(task, this)

//    children += newChildContext

    return newChildContext
  }
}

/*
fun SessionRuntimeContext.newTaskCtx() =
  TaskContext(allSessionsContext, this)
*/

