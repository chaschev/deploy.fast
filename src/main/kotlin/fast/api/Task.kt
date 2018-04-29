package fast.api

import fast.dsl.*
import fast.runtime.DeployFastDI
import fast.runtime.TaskContext
import org.kodein.di.generic.instance

open class Task<R, EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>(
  override val name: String,
  override val desc: String? = null,
  override val extension: EXT
) : ITask<R, EXT, EXT_CONF> {
  override val before by lazy { TaskSet("before") }
  override val after by lazy { TaskSet("after") }

  /** Each task definition belongs to exactly one extension */

  suspend final fun playChild(context: TaskContext<R, EXT, EXT_CONF>): ITaskResult<Any> {
    return context.play(this) as ITaskResult<Any>
  }

  override suspend final fun play(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R> {
    return context.play(this as Task<Any, EXT, EXT_CONF>) as ITaskResult<R>
  }

  override suspend fun doIt(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R> {
    TODO("not implemented")
  }

  class DummyApp : DeployFastApp<DummyApp>("dummy")

  companion object {
    val rootExtension by DeployFastDI.FAST.instance<DeployFastApp<*>>()
    val dummyApp = DummyApp()
    val root = LambdaTask("root", dummyApp, { TaskResult.ok })
    val dummy = LambdaTask("dummy", dummyApp, { TaskResult.ok })
  }
}