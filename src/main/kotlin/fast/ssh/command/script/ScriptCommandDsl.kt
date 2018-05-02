package fast.ssh.command.script

import fast.api.User
import fast.api.UserRights
import fast.api.ext.SymlinksDSL
import fast.ssh.process.Console

open class ScriptCommandDsl<R> : ScriptDslSettings() {
  val commands = ArrayList<ScriptDslSettings>()

  fun untar(file: String, block: (TarCommandDsl.() -> Unit)? = null) {
    val dsl = TarCommandDsl(file)

    if (block != null) dsl.apply(block)

    commands += dsl
  }

  fun capture(name: String? = null, block: ScriptCommandWithCapture<*>.() -> Unit) {
    val dsl = ScriptCommandWithCapture<Any>().apply(block)

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
    paths: List<String>,
    rights: UserRights,
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

  fun sh(command: String, block: (ShellCommandDsl.() -> Unit)? = null) {
    val dsl = ShellCommandDsl(command)

    if (block != null) dsl.apply(block)

  }

  fun symlinks(block: (SymlinksDSL.() -> Unit)) {
    commands += SymlinksDSL().apply(block)
  }

}

class ScriptCommandWithCapture<R>(val name: String? = null) : ScriptCommandDsl<R>() {
  lateinit var processInput: (Console, myText: CharSequence) -> Any

  internal fun _addUser(user: User, block: (AddUserCommandDsl.() -> Unit)? = null): AddUserCommandDsl {
    val dsl = AddUserCommandDsl(user)

    if (block != null) dsl.apply(block)


    return dsl
  }
}