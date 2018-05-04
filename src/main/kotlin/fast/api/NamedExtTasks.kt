package fast.api

import fast.dsl.ServiceStatus
import fast.ssh.command.Version
import kotlin.reflect.KProperty

open class NamedExtTasks<EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>(
  val extension: EXT,
  parentCtx: ChildTaskContext<*, *>
) {
  val extCtx: ChildTaskContext<EXT, EXT_CONF>
  //  lateinit var extension: DeployFastExtension<ExtensionConfig>

  init {
    extCtx = parentCtx.newChildContext(extension.asTask, extension.name) as ChildTaskContext<EXT, EXT_CONF>
  }

  open suspend fun getStatus(): ITaskResult<ServiceStatus> = TODO()
  open suspend fun getVersion(): ITaskResult<Version> = TODO()

  inner class ExtensionTaskDelegate<R>(
    val initBlock: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>
  ) {

    @Volatile
    private var value: LambdaTask<R, EXT, EXT_CONF>? = null

    operator fun getValue(extTasks: NamedExtTasks<EXT, EXT_CONF>, property: KProperty<*>): LambdaTask<R, EXT, EXT_CONF> {
      if (value == null) {
        value = LambdaTask(property.name, extension, initBlock)
      }

      return value!!
    }

    operator fun setValue(extTasks: NamedExtTasks<EXT, EXT_CONF>, property: KProperty<*>, any: Any) {
      TODO("not implemented")
    }
  }

  fun <R> extensionTask(block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>): ExtensionTaskDelegate<R> {
    return ExtensionTaskDelegate(block)
  }


  suspend fun <R> extensionFun(name: String, block: suspend ChildTaskContext<EXT, EXT_CONF>.() -> ITaskResult<R>): ITaskResult<R> {
    /**
     * Extension creates a context and every task is executed inside this context. For each task execution a new task
     */
    return LambdaTask(name, extension, block).play(extCtx)
  }

}