package fast.ssh.command.script

import fast.api.User
import fast.api.UserRights
import fast.api.ext.SymlinksDSL
import fast.ssh.process.Console
import org.bouncycastle.cms.RecipientId.password

open class ScriptCommandDsl<R> : ScriptDslSettings() {
  val lines = ArrayList<ScriptLines>()

  private val capture = ArrayList<ScriptCommandDsl<*>>()

  fun untar(file: String, block: (TarCommandDsl.() -> Unit)? = null) {
    val dsl = TarCommandDsl(file)

    lines += if (block == null) dsl else dsl.apply(block)
  }

  fun capture(block: ScriptCommandWithCapture<*>.() -> Unit) {
    this.capture.add(ScriptCommandWithCapture<Any>().apply(block))
  }

  open fun addUser(user: User, block: (AddUserCommandDsl.() -> Unit)? = null) {
     capture {
      _addUser(user, block)

      if(user.password != null) {
        processInput = {
          if (newIn.contains("UNIX password:"))
            stdin.write(user.password + "\n")
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

    lines += if (block == null) dsl else dsl.apply(block)
  }

  fun sh(command: String, block: (ShellCommandDsl.() -> Unit)? = null) {
    val dsl = ShellCommandDsl(command)

    lines += if (block == null) dsl else dsl.apply(block)
  }

  fun symlinks(block: (SymlinksDSL.() -> Unit)) {
    lines += SymlinksDSL().apply(block)
  }

}

class ScriptCommandWithCapture<R> : ScriptCommandDsl<R>() {
  var processInput: (Console.() -> Unit)? = null

  internal fun _addUser(user: User, block: (AddUserCommandDsl.() -> Unit)? = null): AddUserCommandDsl {
    val dsl = AddUserCommandDsl(user)

    if (block != null) dsl.apply(block)

    lines += dsl

    return dsl
  }
}