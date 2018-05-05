package fast.ssh.command.script

import fast.api.User
import fast.api.UserRights
import fast.api.ext.SymlinksDSL
import fast.ssh.process.Console

open class ScriptCommandDsl<R> : ScriptDslSettings() {
  internal var processing: ((Console, HashMap<String, CaptureHolder>) -> R)? = null

  val commands = ArrayList<ScriptDslSettings>()

  fun settings(block: ScriptDslSettings.() -> Unit) {
    this.apply(block)
  }

  fun wget(file: String, sha1: String, block: (WgetCommandDsl.() -> Unit)? = null) {
    val dsl = WgetCommandDsl(file, sha1)

    if (block != null) dsl.apply(block)

    commands += dsl
  }


  fun untar(file: String, block: (TarCommandDsl.() -> Unit)? = null) {
    val dsl = TarCommandDsl(file)

    if (block != null) dsl.apply(block)

    commands += dsl
  }

  fun capture(name: String? = null, block: ScriptCommandWithCaptureDsl<*>.() -> Unit) {
    val dsl = ScriptCommandWithCaptureDsl<Any>().apply(block)

    this.commands += dsl
  }

  open fun addUser(user: User, block: (AddUserCommandDsl.() -> Unit)? = null) {
    capture {
      commands += _addUser(user, block)

      this.processInput = { console, myText ->
        if (user.password != null) {
          if (myText.contains("UNIX password:"))
            console.stdin.write(user.password + "\n")
        }
      }
    }
  }

  fun rights(
    path: String = "change",
    rights: UserRights,
    paths: List<String> = listOf(path),
    create: Boolean = false,
    recursive: Boolean = true,
    block: (AddRightsCommandDsl.() -> Unit)? = null
  ) {
    val dsl = AddRightsCommandDsl(paths)

    dsl.rights = rights
    dsl.create = create
    dsl.recursive = recursive

    if (block != null) dsl.apply(block)

    commands += dsl
  }

  fun cd(dir: String) = sh("cd $dir")
  fun mkdir(dir: String) = sh("mkdir -p $dir")

  fun sh(command: String, block: (ShellCommandDsl.() -> Unit)? = null) {
    val dsl = ShellCommandDsl(command)

    if (block != null) dsl.apply(block)

    commands += dsl
  }

  fun rawSh(command: String) {
    val dsl = RawShellCommandDsl(command)

    commands += dsl
  }

  fun symlinks(block: (SymlinksDSL.() -> Unit)) {
    commands += SymlinksDSL().apply(block)
  }

  fun processResult(block: ((Console, HashMap<String, CaptureHolder>) -> R)) {
    this.processing = block
  }

}

class ScriptCommandWithCaptureDsl<R>(val name: String? = null) : ScriptCommandDsl<R>() {
  lateinit var processInput: (Console, myText: CharSequence) -> Any

  internal fun _addUser(user: User, block: (AddUserCommandDsl.() -> Unit)? = null): AddUserCommandDsl {
    val dsl = AddUserCommandDsl(user)

    if (block != null) dsl.apply(block)

    return dsl
  }
}