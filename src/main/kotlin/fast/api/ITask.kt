package fast.api

import fast.runtime.TaskContext

interface ITask<R, EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig> {
  val name: String
  val desc: String?
  val extension: EXT?

  suspend fun play(context: TaskContext<Any, EXT, EXT_CONF>): ITaskResult<R>

  val before: TaskSet
  val after: TaskSet
  suspend fun doIt(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R>
}