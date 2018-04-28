package fast.dsl.ext

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.ssh.command.Version
import fast.ssh.run

typealias ZippedAppTaskContext = ChildTaskContext<ZippedAppExtension, ZippedAppConfig>

/**
 * This extension will generate zippedApp project file.
 *
 * Test with:
 *
 * Each installation step is a task, so a plugin user can modify them,
 * i.e. configure Cassandra right after installation
 */
class ZippedAppExtension(
  config: (ZippedAppTaskContext) -> ZippedAppConfig
) : DeployFastExtension<ZippedAppExtension, ZippedAppConfig>(
  "zippedApp", config
) {
  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    ZippedAppTasks(this@ZippedAppExtension, parentCtx)
  }
}


class ZippedAppTasks(ext: ZippedAppExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<ZippedAppExtension, ZippedAppConfig>(ext, parentCtx) {
  val downloadTask by lazy {
    LambdaTask("download", extension) {
      ssh.run(
        cmd = "wget --directory-prefix=${config.tempDir} ${config.url}",
        timeoutMs = config.downloadTimeoutSec * 1000
      )
    }
  }

  val createHomeDirTask by extensionTask {
    ssh.run(cmd = "mkdir -p ${config.appDir}")
  }

  val extractToHomeDirTask by extensionTask {
    val file = config.archiveName

    val cmd =
      when {
        file.endsWith("tar.gz") -> "tar xvfz -C ${config.appDir} ${config.tempDir}/$file"
        file.endsWith("gz") -> "tar xvf -C ${config.appDir} ${config.tempDir}/$file"
        file.endsWith("zip") -> "unzip -d ${config.appDir} ${config.tempDir}/$file"
        file.endsWith("bin") -> TODO("execute bin installer")
        else -> throw IllegalArgumentException("unsupported archive type: $file")
      }
    ssh.run(cmd = cmd)
  }

  val createSymlink by extensionTask {
    if(config.symlinks != null) {
      var r = ok

      for (symlink in config.symlinks!!.symlinks) {
        r += ssh.run(cmd = "ln -s ${symlink.destPath} ${symlink.sourcePath}")
      }

      r
    } else {
      ok
    }
  }



  suspend fun download(): BooleanResult = downloadTask.play(extCtx)

  fun createHomeDir(): BooleanResult = TODO()
  fun extractToHomeDir(): BooleanResult = TODO()
  fun createSymlink(): BooleanResult = TODO()
  fun installService(): BooleanResult = TODO()
  fun verifyInstall(): BooleanResult = TODO()

  override suspend fun getVersion(): ITaskResult<Version> {
    val x = downloadTask
    val y = createHomeDirTask

    return super.getVersion()
  }

  override suspend fun getStatus(): ITaskResult<ServiceStatus> =
    LambdaTask("getStatus", extension) {

      TaskResult(ok = false, value = ServiceStatus.installed)
    }.play(extCtx)

}


data class Symlink(
  val sourcePath: String,
  val destPath: String,
  val rights: UserRights = UserRights.omit
) {

}

class SymlinksDSL {
  val symlinks = ArrayList<Symlink>()

  infix fun String.to(appPath: String): Symlink {
    symlinks += Symlink(this, appPath)
    return symlinks.last()
  }

  infix fun Symlink.with(rights: UserRights): Symlink {
    val link = symlinks.removeAt(symlinks.size - 1).copy(rights = rights)

    symlinks += link

    return link

  }
}


class ZippedAppConfig(
  val name: String,
  val version: String,
  val baseUrl: String,
  var archiveName: String = "$name-$version.tar.gz",
  var url: String = "$baseUrl/$archiveName",
  var savedAchivePath: String = "$name-$version/$archiveName",
  var tempDir: String = "/tmp",
  var appDir: String = "/var/lib",
  var binDir: String = "/usr/local/bin",
  var getVersionCommand: String = "$binDir/$name -v",
  var downloadTimeoutSec: Int = 600
) : ExtensionConfig {
  var symlinks: SymlinksDSL? = null

  fun symlinks(block: SymlinksDSL.() -> Unit) {
    symlinks = SymlinksDSL().apply(block)
  }
}


