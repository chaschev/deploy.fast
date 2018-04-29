package fast.api

import fast.dsl.ChildTaskContext
import fast.dsl.DeployFastExtension
import fast.dsl.ExtensionConfig

/**
 * Extension task creates it's own context which corresponds to extension function
 */
class ExtensionTask<R, EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>(
  name: String,
  extension: EXT,
  desc: String? = null,
  block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>
) : LambdaTask<R, EXT, EXT_CONF>(name, desc, extension, block) {
  /*suspend final fun playExt(context: AnyTaskContext): ITaskResult<R> {
    return context.play(this) as ITaskResult<R>
  }*/

  fun asTask(): Task<R, EXT, EXT_CONF> {
    return LambdaTask(name, desc, extension, block)
  }
}