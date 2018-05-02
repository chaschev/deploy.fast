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
) : ScriptDslSettings(), ScriptLines {
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

  while (pos > 0) {
    pos = this.indexOf(substring, pos + 1)

    if (pos >= 0) r++
  }

  return r
}

fun <R> CharSequence.mapEachTextBlock(
  blockStart: String,
  blockEnd: String,
  start: Int = -1,
  end: Int = this.length,
  block: (start: Int, end: Int) -> R
): List<R> {
  var pos = start
  val list = ArrayList<R>()

  while (true) {
    pos = this.indexOf(blockStart, pos + 1)

    if (pos == -1) break

    val blockEndPos = this.indexOf(blockEnd, pos + blockStart.length)

    if (blockEndPos == -1 || blockEndPos >= end) break

    list += block(pos, blockEndPos)

    pos = blockEndPos + blockEnd.length
  }

  return list
}

class AddUserCommandDsl(
  val user: User
) : ScriptDslSettings(), ScriptLines {
  init {
    sudo = true

  }

  override fun lines(): List<String> {
    val userGroup = if(user.group == null) "" else "-g ${user.group}"

    return listOfNotNull(
      if(user.group != null) " grep -q ${user.group} /etc/group || sudo groupadd ${user.group}" else null,

      "id -u ${user.name} &>/dev/null || sudo useradd -m $userGroup ${user.name}",

      if (user.password == null) null else "passwd ${user.name}"
    )
  }
}

class ShellCommandDsl(
  val command: String
) : ScriptDslSettings(), ScriptLines {
  override fun lines(): List<String> = command.lines()
}

data class CaptureHolder(
  val command: ScriptCommandWithCapture<*>,
  val name: String,
  var result: Any? = null
)

class CommandVisitor(
) {
  fun visit(
    dsl: ScriptCommandDsl<*>,
    lines: ArrayList<String> = ArrayList(),
    captureMap: MutableMap<String, CaptureHolder> = HashMap()
  ) {
    for (command in dsl.commands) {
      when (command) {
        is ScriptLines -> {
          lines += command.lines()
        }
        is ScriptCommandWithCapture<*> -> {
          val index = lines.size

          val holder = CaptureHolder(command,command.name ?: index.toString())

          captureMap[holder.name] = holder

          lines += "echo --- start ${holder.name}"

          visit(command, lines, captureMap)

          lines += "echo --- end ${holder.name}"
        }
      }
    }

  }
}

class ShellScript<R>(
  val dsl: ScriptDsl<R>
) {

  suspend fun execute(console: ConsoleProvider, timeoutMs: Int): CommandResult<R> {
    val lines = ArrayList<String>()
    val captureMap = HashMap<String, CaptureHolder>()

    CommandVisitor().visit(dsl.root, lines, captureMap)

    val x = console.runAndWaitInteractive(
      cmd = lines.joinToString(";\n"),
      processing = ConsoleProcessing(
        process = { con -> dsl.processing.invoke(con, captureMap) },
        consoleHandler = { con ->
          /*
          TODO: fix block extraction
            read start tag, get index,
            don't expect there will be a closing tag
            if it is there, try to find next opening tag
            */
          con.newIn.mapEachNamedTextBlock(
            "--- start ",
            "--- end "
          ) { start, end, blockName, blockText ->
            val holder = captureMap[blockName]!!

            holder.result = holder.command.processInput(con, blockText)
          }
        }
      ),
      timeoutMs = timeoutMs
    )

    return x
  }

  fun <R> CharSequence.mapEachNamedTextBlock(
    blockStart: String,
    blockEnd: String,
    start: Int = -1,
    end: Int = this.length,
    block: (start: Int, end: Int, blockName: String, blockText: CharSequence) -> R
  ): List<R> {
    var pos = start
    val list = ArrayList<R>()

    val str = this

    while (true) {
      pos = this.indexOf(blockStart, pos + 1)

      if (pos == -1) break

      val blockNameEnd = str.indexOf('\n', pos + blockStart.length)
      val blockName = str.substring(pos + blockStart.length, blockNameEnd)

      val blockEndFullname = "$blockEnd $blockName"

      val blockEndPos = this.indexOf(blockEndFullname, pos + blockStart.length)

      if(blockEndPos == -1) {
        list += block(pos, -1, blockName, this.subSequence(pos, length ))
        break
      } else {
        // doesn't make much sense, so fuck it
        if (blockEndPos >= end) break

        list += block(pos, blockEndPos, blockName, this.subSequence(pos, end))

        pos = blockEndPos + blockEnd.length
      }
    }

    return list
  }

}