package fast.api.ext

import fast.api.*
import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.lang.InitLater
import fast.ssh.command.Version
import fast.ssh.command.script.ScriptCommandDsl
import fast.ssh.command.script.ScriptDsl
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.command.script.ScriptDslSettings
import fast.ssh.command.script.ScriptLines
import fast.ssh.run
import honey.lang.append
import honey.lang.mapReplace

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

data class Checksum(
  val sha1: String? = null,
  val sha256: String? = null
) {
  fun utilityName() = when {
    sha1 != null -> "sha1sum"
    sha256 != null -> "sha256sum"
    else -> TODO()
  }

  fun get() = when {
    sha1 != null -> sha1
    sha256 != null -> sha256
    else -> TODO()
  }
}

class ConfigurationEditor() {
  // filepath -> editor: (before) -> after
  val editorMap = HashMap<String, (String) -> String>()

  operator fun plusAssign(editor: Pair<String, (String) -> String>) {
    editorMap[editor.first] = editor.second
  }

  fun files(): List<String> {
    return editorMap.keys.toList()
  }

}

class ZippedAppConfig(
  val name: String,
  val version: String
) : ExtensionConfig {
  var archiveBaseUrl: String by InitLater()
//  var archiveBasename: () -> String = { "$name-$version" }
  //name of the folder inside the archive
  var archivedFolderName: () -> String = { "$name-$version" }
  var archiveName = { "$name-$version.tar.gz" }
  var archiveUrl = { "${archiveBaseUrl}/${archiveName()}" }
  var tempDir = { "/tmp/$name" }
  var appDir = {"/var/local/lib/$name"}
  var otherAppDirs = { emptyList<String>() }
  var editor: ConfigurationEditor = ConfigurationEditor()
  var binDir: String = "/usr/local/bin"
  var getVersionCommand = { "$binDir/$name -v" }
  var downloadTimeoutSec: Int = 600
  var createUser: User? = null
  var symlinks: SymlinksDSL? = null
  var execSymlinks: SymlinksDSL? = null

  lateinit var archiveChecksum: Checksum

  fun symlinks(block: SymlinksDSL.() -> Unit) {
    symlinks = SymlinksDSL().apply(block)
  }


  fun with(block: ZippedAppConfig.() -> Unit): ZippedAppConfig {
    this.apply(block)
    return this
  }

  fun withCustomAppRights(block: ScriptCommandDsl<*>.() -> Unit) {

  }

  fun withSymlinks(rights: UserRights = Rights.executableAll, block: (SymlinksDSL.() -> Unit)) {
    execSymlinks = SymlinksDSL().apply(block)
  }
  /*
  TODO: require section
  fun require(AptTasks.() -> Unit) {

  }*/
}

class ZippedAppTasks(ext: ZippedAppExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<ZippedAppExtension, ZippedAppConfig>(ext, parentCtx) {
  val downloadTask by lazy {
    LambdaTask("download", extension) {
      ssh.run(
        cmd = "wget --directory-prefix=${config.tempDir} ${config.archiveUrl}",
        timeoutMs = config.downloadTimeoutSec * 1000
      )
    }
  }

  /*
   * ok: edit conf
   * ok: set rights
   * ok: fix checksum
   * ok: fix archive extraction
   * TODO: install gradle
   * TODO: install service task (boolean start)
   * TODO: verify version task
   * TODO: take a break
   */
  suspend fun install() = extensionFun("install") {
    val appUser = config.createUser ?: User(ssh.user())
    val appPath = config.appDir()
    val tmpDir = config.tempDir()
    val extractedTmpHomePath = "$tmpDir/${config.archivedFolderName()}"

    val tmpConfPath = "$tmpDir/zippedExtConf"

    ScriptDsl.script {
      settings {
        abortOnError = true
      }

      mkdirs(tmpDir)

      cd(tmpDir)

      wget(
        config.archiveUrl(),
        config.archiveChecksum
      )

      untar(
        file = "$tmpDir/${config.archiveName()}"
      )

      if (appUser != null)
        addUser(appUser)

      var appRights = Rights.userOnlyReadWriteFolder
      if (appUser != null) appRights = appRights.copy(owner = appUser)

      rights(
        paths = listOf(appPath).append(config.otherAppDirs()),
        rights = appRights,
        create = true,
        recursive = false
      ) { sudo = true; abortOnError = true }

      sh("cp -R $extractedTmpHomePath/* $appPath") {
        withUser = appUser?.name
        abortOnError = false
      }

      mkdirs(tmpConfPath)

      if(!config.editor.files().isEmpty()) {
        sh("echo copying conf files into $tmpConfPath")
        sh("cp -f ${config.editor.files().joinToString(" ")} $tmpConfPath") { sudo = true }
      }

      rights(
        path = tmpConfPath,
        rights = Rights.writeAll
      ) { sudo = true }

      if (config.execSymlinks != null) {
        val symlinks = config.execSymlinks!!

        //create paths for symlinks
        val parentPaths = symlinks.symlinks.map { "$appPath/${it.sourcePath}".substringBeforeLast('/') }

        symlinks.sudo = true

        symlinks.symlinks.mapReplace { it.copy(sourcePath = "$appPath/${it.sourcePath}") }

        //add symlink creation to script
        commands += symlinks

        // set right to exec sources
        rights(
          paths = parentPaths.append(symlinks.symlinks.map { it.sourcePath }),
          rights = (symlinks.rights ?: Rights.executableAll).copy(owner = appUser)
        ) {
          sudo = true
          abortOnError = true
        }
      }

      ok

    }.execute(ssh)

    /*

    PATCH & UPLOAD CONF

    first: files are copied
    then: script is generated
    last: script is executed

    */
    if(config.editor.files().isNotEmpty()) {
      script {
        config.editor.editorMap.forEach { (sourcePath, editor) ->
          val basename = sourcePath.substringAfterLast('/')
          val contentsBefore = ssh.files().readAsString("$tmpConfPath/$basename")
          val contentsAfter = editor(contentsBefore)

          sh("echo writing conf files after editing $tmpConfPath")

          ssh.files().writeToString("$tmpConfPath/$basename", contentsAfter)

          sh("cp -f $tmpConfPath/$basename $sourcePath") { sudo = true }

          var confRights = Rights.userReadWrite
          if (appUser != null) confRights = confRights.copy(owner = appUser)

          rights(
            path = sourcePath,
            rights = confRights
          ) { sudo = true }
        }
      }.execute(ssh)
    }

    TaskResult.ok
  }

  override suspend fun getVersion(): ITaskResult<Version> {

    return super.getVersion()
  }

  override suspend fun getStatus(): ITaskResult<ServiceStatus> =
    LambdaTask("getStatus", extension) {

      TaskResult(value = ServiceStatus.installed, ok = false)
    }.play(extCtx)

}

data class Symlink(
  val sourcePath: String,
  val destPath: String,
  val rights: UserRights = UserRights.omit
) {

}

class SymlinksDSL : ScriptDslSettings(), ScriptLines {
  val symlinks = ArrayList<Symlink>()

  var rights: UserRights? = null

  infix fun String.to(appPath: String): Symlink {
    symlinks += Symlink(appPath, this)
    return symlinks.last()
  }

  infix fun Symlink.with(rights: UserRights): Symlink {
    val link = symlinks.removeAt(symlinks.size - 1).copy(rights = rights)

    symlinks += link

    return link

  }

  override fun lines(): List<String> {
    return symlinks.map {
      "ln -fsn ${it.sourcePath} ${it.destPath}"
    }
  }
}





