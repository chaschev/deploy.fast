package fast.ssh.command.script

import fast.ssh.SshProvider
import fast.ssh.execute

class ScriptDsl<R>(val root: ScriptCommandDsl<R>) : ScriptDslSettings() {
  val resultProcessing by lazy { root.resultProcessing }
  val consoleProcessing by lazy { root.consoleProcessing }

  fun asScript() = ShellScript(this)

  suspend fun execute(ssh: SshProvider): ScriptCommandResult<R> {
    require(resultProcessing != null, { "processing field must be set in dsl with processing. I.e. return true, we don't give a shit" })

    return ssh.execute(this)
  }

  companion object {
    fun <R> processScript(block: ScriptCommandDsl<R>.() -> Unit): ScriptDsl<R> {
      return ScriptDsl(
        ScriptCommandDsl<R>().apply(block)
      )
    }

    fun script(block: ScriptCommandDsl<Boolean>.() -> Unit): ScriptDsl<Boolean> {
      ScriptCommandDsl<Boolean>().let { dsl ->
        dsl.apply(block)

        if(dsl.resultProcessing == null)
          dsl.resultProcessing = { console, _ -> console.result.isOk()}

//        if(dsl.consoleProcessing == null)
//          dsl.consoleProcessing = { console, _ -> console.result.isOk()}

        return ScriptDsl(dsl)
      }
    }
  }
}