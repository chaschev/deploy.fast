package fast.dsl

import fast.api.TaskSet
import fast.runtime.AllSessionsRuntimeContext

/**
 * TODO: rename ext into i.e. extensions,
 */
open class DeployFastDSL<CONF : ExtensionConfig, EXT : DeployFastExtension<EXT, CONF>>(
  val ext: EXT
) {
  internal var info: InfoDSL? = null
  internal var ssh: SshDSL? = null

  val tasks: TaskSet = TaskSet(
    name = ext.name,
    desc = "Tasks of extension ${ext.name}")

  val globalTasks: TaskSet = TaskSet("${ext.name}.global",
    "Global Tasks for Extension $ext"
  )

  fun autoInstall(): Unit = TODO()

  fun info(block: InfoDSL.() -> Unit) {
    info = InfoDSL().apply(block)
  }

  private var beforeGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null
  private var afterGlobalTasks: (AllSessionsRuntimeContext.() -> Unit)? = null

  fun ssh(block: SshDSL.() -> Unit) {
    ssh = SshDSL().apply(block)
  }

  fun globalTasksBeforePlay(block: TasksDSL<EXT, CONF>.() -> Unit) {
    globalTasks.addAll(TasksDSL<EXT, CONF>().apply(block).taskSet)
  }


  /*fun beforeGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    beforeGlobalTasks = block
  }

  fun afterGlobalTasks(block: AllSessionsRuntimeContext.() -> Unit) {
    afterGlobalTasks = block
  }*/

  fun play(block: TasksDSL<EXT, CONF>.() -> Unit) {
    tasks.addAll(TasksDSL<EXT, CONF>().apply(block).taskSet)
  }

//  fun beforePlay(block: TasksDSL.() -> Unit) {
//    tasks.before.addAll(TasksDSL().apply(block).taskSet)
//  }
//
//  fun afterPlay(block: TasksDSL.() -> Unit) {
//    tasks.after.addAll(TasksDSL().apply(block).taskSet)
//  }


  companion object {
    fun <CONF : ExtensionConfig, EXT : DeployFastExtension<EXT, CONF>> createExtDsl(
      ext: EXT, block: DeployFastDSL<CONF, EXT>.() -> Unit

    ): DeployFastDSL<CONF, EXT> {
      val deployFastDSL = DeployFastDSL(ext)

      deployFastDSL.apply(block)

      return deployFastDSL
    }

    fun <APP : DeployFastApp<APP>> createAppDsl(
      app: APP, block: DeployFastAppDSL<APP>.() -> Unit

    ): DeployFastAppDSL<APP> {
      val deployFastDSL = DeployFastAppDSL(app)

      deployFastDSL.apply(block)

      return deployFastDSL
    }
  }
}