package fast.dsl


class ZippedAppTasks : NamedExtTasks() {
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
) {
  fun symlinks(block: SymlinksDSL.() -> Unit): SymlinksDSL {
    return SymlinksDSL().apply(block)
  }
}

class ZippedAppExtension: DeployFastExtension() {
  override val tasks: ZippedAppTasks = ZippedAppTasks()

  lateinit var config: ZippedAppConfig

  fun configure(
    name: String,
    version: String,
    baseUrl: String,
    block: ZippedAppConfig.() -> Unit){

    config = ZippedAppConfig(name, version, baseUrl).apply(block)
  }
}