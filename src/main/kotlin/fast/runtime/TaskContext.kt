package fast.runtime

import fast.api.DeployFastExtension
import fast.api.ExtensionConfig
import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.api.ITaskResult
import fast.api.Task
import fast.runtime.DeployFastDI.FAST
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import fast.log.KLogging
import org.kodein.di.generic.instance
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

data class TaskInterceptor<EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>(
  var modifyConfig: (EXT_CONF.() -> Unit)? = null,
  var modifyExtension: (EXT.() -> Unit)? = null
) {
  fun config(block: EXT_CONF.() -> Unit) {
    modifyConfig = block
  }

  companion object {
    fun <EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>
      intercept(block: TaskInterceptor<EXT, EXT_CONF>.() -> Unit) =

      TaskInterceptor<EXT, EXT_CONF>().apply(block)
  }
}

// The difficult part:
//  context of a task is created inside the tasks() call and is stored inside NamedExtTasks object
class TaskContext<R, EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig>
(
  val task: Task<R, EXT, EXT_CONF>,
  val session: SessionRuntimeContext,
  val parent: TaskContext<*, *, *>?,
  var path: String = getPath(parent, task)
) {
  val ssh = session.ssh

  val global: AllSessionsRuntimeContext by FAST.instance()
  val app: AppContext by FAST.instance()

  lateinit var config: EXT_CONF
  lateinit var extension: EXT

  private val children = ArrayList<TaskContext<Any, EXT, EXT_CONF>>()

  // job is required for cancellation and coordination between tasks
  @Volatile
  private var job: Deferred<ITaskResult<Any>>? = null

  val address: String by lazy { session.host.address }

  /**
   * Api to play a Task
   *
   * That's the only entry point for playing tasks!
   *
   * RIGHT:
   *  So we are in Parent Extension, i.e. Java
   *   we call child task from extension apt, i.e. apt.install('java')
   *   a new context will be created
   *   the task will be called inside this new context
   *
   * WRONG (current situation):
   * So we are in Parent Extension, i.e. Java
   *   we call child task from extension apt, i.e. apt.install('java')
   *   we don't know fuck about it's types, so it is Task<Any, *, *>
   */
  private suspend fun playOneTask(
    childTask: Task<Any, *, ExtensionConfig>,
    interceptors: TaskInterceptor<EXT, EXT_CONF>? = null
  ): ITaskResult<Any> {
    job = asyncNoisy {
      val childContext = newChildContext(childTask, interceptors = interceptors)

      logger.info { "playing task: ${childContext.path}" }

      //types are wrong, but so far I don't know how to replace *
      (childTask as Task<Any, EXT, EXT_CONF>).doIt(childContext)
    }

    return job!!.await()
  }

  /**
   * @param customName custom names are for new extension context, i.e.
   * task1.task2.apt::listPackages
   *             ^-- that is the custom name
   * @param
   */
  internal fun newChildContext(
    childTask: Task<*, *, *>,
    customName: String? = null,
    interceptors: TaskInterceptor<EXT, EXT_CONF>? = null
  ): TaskContext<Any, EXT, EXT_CONF> {
    val childSession = session.newChildContext(childTask)

    // that is not correct when there is an extension change
    val childContext = TaskContext<Any, EXT, EXT_CONF>(
      childTask as Task<Any, EXT, EXT_CONF>, childSession, this
    )

    if (customName != null)
      childContext.path = "$path.$customName"

    logger.debug { "created new context: ${childContext.path}" }

    this.children.add(childContext)

    // almost everything is initialised before execution
    // now, update vars extension's config
    // and then start

    childContext.config = childTask.extension.config(childContext)
    childContext.extension = childTask.extension

    if (interceptors != null)
      childContext.applyInterceptors(interceptors)
    // TODO apply interceptors here (to change config variables)

    return childContext
  }

  private fun applyInterceptors(interceptor: TaskInterceptor<EXT, EXT_CONF>) {
    with(interceptor) {
      if (modifyConfig != null) config.apply(modifyConfig!!)
      if (modifyExtension != null) extension.apply(modifyExtension!!)
    }
  }

  /*
  EXTENSION PLAY CHANGE!
  suspend fun playExtensionTask(task: AnyExtensionTask): ITaskResult<Any> {
    val childCtx = newChildContext(task, task.extension!!.name)
    return childCtx.play(task.asTask())
  }*/

  suspend fun play(
    task: Task<R, EXT, EXT_CONF>,
    interceptors: (TaskInterceptor<EXT, EXT_CONF>)? = null
  ): ITaskResult<R> {
    return playChildTask(task as AnyTask) as ITaskResult<R>
  }

  suspend fun playChildTask(
    childTask: AnyTask,
    interceptors: TaskInterceptor<EXT, EXT_CONF>? = null
  ): ITaskResult<*> {
    // TODO apply interceptors here (modify tasks)
//    if(childTask is ExtensionTask) return playExtensionTask(childTask)

    var result: ITaskResult<*> = ok
    var taskResult: ITaskResult<*>? = null

    with(childTask) {
      if (before.size() > 0) {
        taskResult = play(before, interceptors)
        result *= taskResult!!
      }

      taskResult = playOneTask(this, interceptors)

      result *= taskResult!!

      if (after.size() > 0) {
        result *= play(after, interceptors)
      }
    }

    return if (result.ok == taskResult?.ok) taskResult!! else result
  }

  suspend fun play(dsl: DeployFastDSL<*, *>): ITaskResult<*> {
    //that will go TaskSet/Task -> play -> iterate -> play each child
    return play(dsl.tasks)
  }

  suspend fun play(
    tasks: Iterable<AnyTask>,
    interceptors: (TaskInterceptor<EXT, EXT_CONF>)? = null
  ): AnyResult {
    var r: AnyResult = TaskResult.ok

    for (task in tasks) {
      r *= playChildTask(task, interceptors)
    }

    return r
  }

  fun getStringVar(name: String): String {
    return session.host.getVar(name) as String
  }

  val user by lazy { ssh.user() }
  val home by lazy { ssh.home }

  fun <T : TaskContext<*, *, *>> getParentCtx(aClass: KClass<T>): T? {
    var ctx: TaskContext<*, *, *>? = parent

    while (ctx != null) {
      if (ctx::class.isSubclassOf(aClass)) return ctx as T
      ctx = ctx.parent
    }

    return null
  }

  fun <T : TaskContext<*, *, *>> getParentCtx(block: (TaskContext<*, *, *>) -> Boolean): T? {
    var ctx: TaskContext<*, *, *>? = parent

    while (ctx != null) {
      if (block(ctx)) return ctx as T
      ctx = ctx.parent
    }

    return null
  }

  suspend fun
    distribute(
    name: String,
    timeoutMs: Long = 600_000,
    await: Boolean = false,
    block: GlobalMap.DistributedJobDsl<EXT, EXT_CONF>.() -> Unit
  ): GlobalMap.DistributeResult<EXT, EXT_CONF> {
    return app.globalMap.distribute(name, this as TaskContext<*, EXT, EXT_CONF>, block, await, timeoutMs)
  }

  companion object : KLogging()

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

private fun <EXT : DeployFastExtension<EXT, EXT_CONF>, EXT_CONF : ExtensionConfig, R> getPath(parent: TaskContext<*, *, *>?, task: Task<R, EXT, EXT_CONF>): String {
  return if (parent?.path?.isEmpty() ?: true) task.name else "${parent!!.path}.${task.name}"
}