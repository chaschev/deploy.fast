package fast.api

import fast.dsl.*
import fast.runtime.DeployFast
import fast.runtime.TaskContext
import fast.runtime.TaskInterceptor
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

  override suspend final fun play(
    context: ChildTaskContext<EXT, EXT_CONF>
  ): ITaskResult<R> {
    return context.play(this as Task<Any, EXT, EXT_CONF>) as ITaskResult<R>
  }

  /**
   * Example:
   *
   * with(extension.stash.tasks(this)) {
   *   stash.playWithInterception(extCtx, intercept {
   *     config {
   *       files.addAll(artifacts.value)
   *     }
   *   })
   * }
   */
  override suspend final fun playWithInterception(
    context: ChildTaskContext<EXT, EXT_CONF>,
    interceptors: TaskInterceptor<EXT, EXT_CONF>
  ): ITaskResult<R> {

    return context.play(this as Task<Any, EXT, EXT_CONF>, interceptors) as ITaskResult<R>
  }

  override suspend fun doIt(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R> {
    TODO("not implemented")
  }

  class DummyApp : DeployFastApp<DummyApp>("dummy")

  companion object {
    val rootExtension by DeployFast.FAST.instance<DeployFastApp<*>>()
    val dummyApp = DummyApp()
    val root = LambdaTask("root", dummyApp, { TaskResult.ok })
    val dummy = LambdaTask("dummy", dummyApp, { TaskResult.ok })
  }
}