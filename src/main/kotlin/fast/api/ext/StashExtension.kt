package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.runtime.TaskContext
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.run
import mu.KLogging
import java.io.File

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
  val stashFolder: String,
  val owners: ArrayList<String> = ArrayList(),
  val files: ArrayList<String> = ArrayList()
) : ExtensionConfig {
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

  suspend fun copyKey() = extensionFun("copyKey") {
    val keyPath = app.inventory.sshConfig.keyPath ?: return@extensionFun ok

    distribute("stash.copyKey") {
      app.addresses() with {
        script {
          mkdirs("${ssh.home}/.ssh")
          sh("rm -f ${ssh.home}/.ssh/fast_id")
        }.execute(ssh)

        ssh.files().copyLocalFiles("${ssh.home}/.ssh/fast_id", File(keyPath))
        ssh.files().chmod("${ssh.home}/.ssh/fast_id", mod = "u=r,go=")

        ok
      }
    }.await()

    ok
  }

  @Suppress("IMPLICIT_CAST_TO_ANY")
  suspend fun stash(id: String, owners: List<String>? = null, files: List<String>? = null) = extensionFun("stashTask") {
    if (files != null) {
      config.files.addAll(files)
    }

    if (owners != null) {
      config.owners.addAll(owners)
    }

    val stashConfig = config

    copyKey()

    println("files: ${stashConfig.files}, owners: ${config.owners}")

    (if (!config.owners.contains(address)) {
      distribute("stash.copyFiles").await()
    } else {
      distribute("stash.copyFiles") {
        stashConfig.owners.take(1) with {
          println("files: ${stashConfig.files}")
          app.globalMap["stash.files.$id"] = stashConfig.files

          var r: ITaskResult<*> = ssh.run("cp ${stashConfig.files.joinToString(" ")} ${stashConfig.stashFolder}")

          // todo: verify checksum via global map
          // todo: provide ssh auth
          for (host in stashConfig.hosts) {
            if (host.address == address) continue

            r *= ssh.run("scp -i ${ssh.home}/.ssh/fast_id -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no ${stashConfig.stashFolder}/* ${host.address}:${stashConfig.stashFolder}")
          }

          r
        }
      }.await()
    } as ITaskResult<Any>?)?.asBoolean() ?: ok
  }

  suspend fun unstash(id: String) = extensionFun("unstash") {
    val files = app.globalMap["stash.files.$id"] as List<String>

    script {
      files
        .map { it.substringAfterLast('/') to it }
        .forEach { (name, destPath) ->
          sh("cp ${config.stashFolder}/$name $destPath")
        }
      sh("rm -f ${config.stashFolder}/*")
    }.execute(ssh).toFast()

    ok
  }


  suspend fun doIt() = extensionFun("doIt") {
    logger.info { "stashing-unstashing" }


    ok
  }


  companion object : KLogging()
}


