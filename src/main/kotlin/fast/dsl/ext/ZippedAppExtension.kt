package fast.dsl.ext

import fast.dsl.*
import fast.runtime.AppContext
import fast.runtime.TaskContext

/*

class ZippedAppTasks(ext: ZippedAppExtension, taskCtx: TaskContext) : NamedExtTasks(
  ext as DeployFastExtension<ExtensionConfig>, taskCtx
) {
  fun install() : Task = TODO()
  fun verifyInstall(): Task = TODO()
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
  var savedAchivePath:String = "$name-$version/$archiveName",
  var tempDir: String = "/tmp",
  var appDir: String = "/var/lib",
  var binDir: String = "/usr/local/bin"
) : ExtensionConfig {
  fun symlinks(block: SymlinksDSL.() -> Unit): SymlinksDSL {
    return SymlinksDSL().apply(block)
  }
}

class ZippedAppExtension(
  app: AppContext,
  config: (TaskContext) -> ZippedAppConfig
): DeployFastExtension<ZippedAppConfig>("zippedApp", config) {
  override val tasks: (TaskContext) -> ZippedAppTasks = {ZippedAppTasks(this@ZippedAppExtension, it)}
}*/
