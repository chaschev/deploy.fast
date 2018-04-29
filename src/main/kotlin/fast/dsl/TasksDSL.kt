package fast.dsl

import fast.api.ITaskResult
import fast.api.LambdaTask
import fast.api.TaskSet
import fast.runtime.DeployFastDI
import org.kodein.di.generic.instance

class TasksDSL<EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig> {
  //  private val tasks = ArrayList<Task>()
  internal val taskSet = TaskSet()

  val app = DeployFastDI.FASTD.instance<DeployFastApp<*>>() as EXT

  fun init(block: () -> Unit) = block.invoke()

  fun task(
    name: String = "",
    block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<*>
  ): Unit {
    taskSet.append(LambdaTask<Any, EXT, EXT_CONF>(name, app, block as suspend (ChildTaskContext<EXT, EXT_CONF>) -> ITaskResult<Any>) as AnyTask)
  }

//  infix fun String.task(block: TaskContext.() -> TaskResult) = task(this, block)
}