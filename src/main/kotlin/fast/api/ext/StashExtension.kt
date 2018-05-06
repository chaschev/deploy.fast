package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.runtime.TaskContext
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.run
import mu.KLogging

typealias StashTaskContext = ChildTaskContext<StashExtension, StashConfig>

class StashExtension(
  config: (TaskContext<*, *, *>) -> StashConfig
) : DeployFastExtension<StashExtension, StashConfig>(
  "stash", config
) {
  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    StashTasks(this@StashExtension, parentCtx)
  }
}

/**
 * A simple version of stash: one party owns all files to transfer
 * More sophisticated version requires leader election. It can be reused in several scenarios, i.e.
 *  - parallel jobs
 *  - stash
 * An easy way to elect leaders is to choose them in app.runOnce section
 */
class StashConfig(
  val hosts: List<Host> = ArrayList(),
  hostConfigs: List<StashHost>? = null,
  val stashFolder: String,
  val owners: ArrayList<String> = ArrayList(),
  val files: ArrayList<String> = ArrayList()
) : ExtensionConfig {
  val hostConfigs = hostConfigs ?: hosts.map { StashHost(this, it) }
}


class StashHost(
  parent: StashConfig,
  val host: Host,
  val hostname: String = host.name,
  val stashFolder: String = parent.stashFolder
) {

}

class StashTasks(ext: StashExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<StashExtension, StashConfig>(ext, parentCtx) {

  suspend fun stash(owners: List<String>? = null, files: List<String>? = null) = extensionFun("stashTask") {
    if(files != null) {
      config.files.addAll(files)
    }

    if(owners != null) {
      config.owners.addAll(owners)
    }

    val eligible = config.owners.contains(address)
    val taskKey = "stash_$path"


    if (!eligible) {
      app.awaitKey(taskKey)
      app.runOnce(taskKey, { ok })
    } else {
      app.runOnce(taskKey) {
        var r: ITaskResult<*> = ssh.run("cp ${config.files.joinToString(" ")} ${config.stashFolder}")

        // todo: verify checksum via global map
        // todo: provide ssh auth
        for (host in config.hosts) {
          if (host.address == address) continue

          r *= ssh.run("scp ${config.stashFolder}/* ${host.address}:${config.stashFolder}")
        }

        r
      }

    } as ITaskResult<Any>
  }

  suspend fun unstash() = extensionFun("unstash") {
    script {
      config.files
        .map { it.substringAfterLast('/') to it }
        .forEach { (name, destPath) ->
          sh("cp ${config.stashFolder}/$name $destPath")
        }
      sh("rm ${config.stashFolder}/*")
    }.execute(ssh).toFast()

    ok
  }


  suspend fun doIt() = extensionFun("doIt") {
    logger.info { "stashing-unstashing" }


    ok
  }


  companion object : KLogging()
}


