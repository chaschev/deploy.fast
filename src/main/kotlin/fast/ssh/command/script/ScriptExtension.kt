package fast.ssh.command.script

import fast.api.User
import fast.ssh.*
import fast.ssh.command.CommandResult

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

class TarCommandDsl(
  val file: String
): ScriptDslSettings(), ScriptLines {
  override fun lines(): List<String> {
    return when {
      file.endsWith("tar.gz") -> listOf("tar xfz $file")
      file.endsWith("gz") -> listOf("tar xf $file")
      else -> throw Exception("todo: support $file for tar")
    }
  }
}

fun CharSequence.countEntries(substring: String): Int {
  var pos = -1
  var r = 0

  while(pos > 0) {
    pos = this.indexOf(substring, pos + 1)

    if( pos >= 0) r++
  }

  return r
}

class AddUserCommandDsl(
  val user: User
): ScriptDslSettings(), ScriptLines {
  init {
    sudo = true

  }

  override fun lines(): List<String> {
    return listOfNotNull(
      "id -u test &>/dev/null || sudo useradd -m -g ${user.group} $user",
      if(user.password ==null) null else "passwd ${user.name}"
    )
  }
}

class ShellCommandDsl(
  val command: String
): ScriptDslSettings(), ScriptLines {
  override fun lines(): List<String> = command.lines()
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