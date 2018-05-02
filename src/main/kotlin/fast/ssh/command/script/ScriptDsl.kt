package fast.ssh.command.script

import fast.ssh.SshProvider
import fast.ssh.execute
import fast.ssh.process.Console

class ScriptDsl<R>(val root: ScriptCommandDsl<R>) : ScriptDslSettings() {
  lateinit var processing: ((Console, HashMap<String, CaptureHolder>) -> R)

  fun asScript() = ShellScript(this)

  suspend fun execute(ssh: SshProvider) =  ssh.execute(this)


  companion object {
    fun <R> script(block: ScriptCommandDsl<R>.() -> Unit): ScriptDsl<R> {
      return ScriptDsl(
       ScriptCommandDsl<R>().apply(block)
      )
    }
  }


}