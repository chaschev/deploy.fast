package fast.ssh.command.script

import fast.ssh.ConsoleProcessing
import fast.ssh.SshProvider
import fast.ssh.execute

class ScriptDsl<R>(val root: ScriptCommandDsl<R>) : ScriptDslSettings() {

  lateinit var processing: ConsoleProcessing<R>

//  fun command(block: ScriptCommandDsl.() -> Unit) {
//    this.commands.add(ScriptCommandDsl().apply(block))
//  }

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