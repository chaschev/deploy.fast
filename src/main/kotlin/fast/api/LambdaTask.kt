package fast.api

import fast.dsl.ChildTaskContext
import fast.dsl.DeployFastExtension
import fast.dsl.ExtensionConfig

open class LambdaTask<R, EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>(
  name: String,
  desc: String? = null,
  extension: EXT,
  val block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>
) : Task<R, EXT, EXT_CONF>(name, desc, extension) {

  constructor(
    name: String,
    extension: EXT,
    block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>
  )
    : this(name, null, extension, block)

  override suspend fun doIt(context: ChildTaskContext<EXT, EXT_CONF>): ITaskResult<R> {
    return block.invoke(context)
  }
}