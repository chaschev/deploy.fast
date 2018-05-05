package fast.ssh.command.script

import fast.ssh.SshProvider
import fast.ssh.command.CommandResult
import fast.ssh.execute

class ScriptDsl<R>(val root: ScriptCommandDsl<R>) : ScriptDslSettings() {
  val processing by lazy { root.processing }

  fun asScript() = ShellScript(this)

  suspend fun execute(ssh: SshProvider): ScriptCommandResult<R> {
    require(processing != null, { "processing field must be set in dsl with processing. I.e. return true, we don't give a shit" })
    return ssh.execute(this)
  }

  companion object {
    fun <R> processScript(block: ScriptCommandDsl<R>.() -> Unit): ScriptDsl<R> {
      return ScriptDsl(
        ScriptCommandDsl<R>().apply(block)
      )
    }

    fun script(block: ScriptCommandDsl<Boolean>.() -> Unit): ScriptDsl<Boolean> {
      val commandDsl = ScriptCommandDsl<Boolean>()

      commandDsl.apply(block)

      if(commandDsl.processing == null)
        commandDsl.processing = {console, _ -> console.result.isOk()}

      return ScriptDsl(commandDsl)
    }
  }
}