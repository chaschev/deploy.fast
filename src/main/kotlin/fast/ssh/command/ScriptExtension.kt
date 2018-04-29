package fast.ssh.command

import fast.ssh.*

/**
 Script is used to have multiple commands for the same packet sent to the server

 On sudo
   https://unix.stackexchange.com/questions/176997/sudo-as-another-user-with-their-environment

 How to do

 ScriptBuilder is a list of commands
 CommandBuilder is a block script to capture output.
 LineBuilder(sudo, withUser, withPassword, tty, dir) -
  a block of code to execute. Check if the dir is changed

 script {
  sudo = true
  dir = '.'
  abortOnError = false (default)

  command("service cassandra restart") {
    dir = '.'
    sudo = true
    user = 'vagrant'
    promptCallback =
    before = {}
    process = (console, myText) -> R
    processError = ...
  }

  commands() {
    line "tar xvfz $archive --directory=$dir"
    setRights dir, userRights      # translates into two lines
  }

 }

How to separate output from one another stdout+stderr

 echo "assholes don't like what we are trying to establish here $timestamp"
 echo "my timestamp bitch $? $PPID $timestamp"

 */

class CommandTranslationStrategy(/*settings*/)
class ScriptExecutionStrategy(/*settings*/)

open class ScriptDslSettings {
  var sudo = false
  var dir = ""
  var abortOnError = true

  var withUser: String? = null
  var withPassword: String? = null
  var passwordPromptRegex: Regex? = null

  fun copySettings(other: ScriptDslSettings) {
    sudo = other.sudo
    dir  = other.dir
    abortOnError = other.abortOnError
    withUser = other.withUser
    withPassword = other.withPassword
    passwordPromptRegex = other.passwordPromptRegex
  }
}

class ScriptDsl<R> : ScriptDslSettings() {
  private val commands = ArrayList<ScriptCommandDsl>()

  lateinit var processing: ConsoleProcessing<R>

  fun command(block: ScriptCommandDsl.() -> Unit) {
    this.commands.add(ScriptCommandDsl().apply(block))
  }

  fun asScript() = ShellScript(this)

  suspend fun execute(ssh: SshProvider) =  ssh.execute(this)


  companion object {
    fun <R> script(block: ScriptDsl<R>.() -> Unit): ScriptDsl<R> {
      return ScriptDsl<R>().apply(block)
    }
  }


}

class ScriptCommandDsl : ScriptDslSettings(){

}

class ScriptLineDsl : ScriptDslSettings(){

}

class ShellScript<R>(
  val dsl: ScriptDsl<R>
) {

  fun asText(): String = TODO()


  suspend fun execute(console: ConsoleProvider, timeoutMs: Int): CommandResult<R> {
    return console.runAndWaitInteractive(
      asText(),
      dsl.processing,
      timeoutMs
    )

    /*
    start cd tracking

    for(command in commands) {
     copy script settings into command
     echo start of input

     for(line in command) {
       copy command settings into line
       if(dir changed) print cd, change current dir
     }

     echo end of input

     if(abortOnError && error) exitWithError(command, console, exitCode)
    }
     */

    /*
    output processing
     sudo password prompt rule: if there is password provided by user (we don't know how his system works, how long is the installation and for how long time his role is active)
       echo $timestamp password bitch

     */

    TODO()
  }
}