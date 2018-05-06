package fast.ssh.command.script

import fast.api.User
import fast.ssh.*
import fast.ssh.command.CommandResult
import fast.ssh.process.Console
import java.util.concurrent.atomic.AtomicReference

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
      file.endsWith("zip") -> listOf("unzip $file")
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
    val userGroup = if (user.group == null) "" else "-g ${user.group}"

    return listOfNotNull(
      if (user.group != null) " grep -q ${user.group} /etc/group || sudo groupadd ${user.group}" else null,

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

class RawShellCommandDsl(
  val command: String
) : ScriptDslSettings(), ScriptBlock {
  override fun getString(): String = command
}

data class CaptureHolder(
  val command: ScriptCommandWithCaptureDsl<*>,
  val id: String,
  var userName: String? = null,
  var result: Any? = null
) {
  var text: CharSequence? = null
  var start: Int = -1
}

class CommandVisitor {
  fun visit(
    dsl: ScriptCommandDsl<*>,
    lines: ArrayList<String> = ArrayList(),
    captureMap: CaptureMap
  ) {
    for (command in dsl.commands) {
      when (command) {
        is ScriptLines -> {
          lines += command.lines().map {
            command.withSudoPrefix(it)
          }

          val prevCommand = lines.last().cuteCut(20)

          if (command.abortOnError) {
            lines += "rc=\$?; if [ \$rc != 0 ] ; then echo process exited with code \$rc, exiting command: $prevCommand, line: ${lines.size};  exit \$rc; fi"
          }
        }
        is ScriptBlock -> {
          lines += command.getString()
        }
        is ScriptCommandWithCaptureDsl<*> -> {
          val index = lines.size

          val holder = CaptureHolder(command, index.toString(), command.name)

          captureMap.add(holder)

          lines += "echo --- start ${holder.id}"

          visit(command, lines, captureMap)

          lines += "echo --- end ${holder.id}"
        }
      }
    }

  }
}

private fun ScriptDslSettings.withSudoPrefix(line: String): String {
  return if (withUser != null) {
    "sudo -u $withUser $line"
  } else
    if (sudo) {
      "sudo $line"
    } else
      line
}

class ScriptCommandResult<R>(
  console: Console,
  exception: Exception? = null,
  val captureMap: CaptureMap
) : CommandResult<R>(console, exception) {

  operator fun get(name: String): CaptureHolder? {
    return captureMap.nameMap[name]
  }

  constructor(
    captureMap: CaptureMap,
    r: CommandResult<R>
  ) : this(r.console, r.exception, captureMap) {

    this._errors = ArrayList(r.errors())
    this.value = r.value
  }
}

class CaptureMap {
  fun add(holder: CaptureHolder) {
    idMap[holder.id] = holder
    if (holder.userName != null) nameMap[holder.userName!!] = holder
  }

  val idMap = HashMap<String, CaptureHolder>()
  val nameMap = HashMap<String, CaptureHolder>()

}

class ShellScript<R>(
  val dsl: ScriptDsl<R>
) {

  val captureMap = CaptureMap()

  suspend fun execute(ssh: ConsoleProvider, timeoutMs: Int): ScriptCommandResult<R> {
    val lines = ArrayList<String>()

    CommandVisitor().visit(dsl.root, lines, captureMap)

    val currentCapture = AtomicReference<CaptureHolder>(null)

    val x = ssh.runAndWaitInteractive(
      cmd = lines.joinToString("\n"),
      processing = ConsoleProcessing(
        process = { console ->
          captureProcessing(console)
          dsl.allCapturesProcessing!!.invoke(console, captureMap)
        },
        consoleHandler = { con ->
          val newHolders = con.newIn.mapEachNamedTextBlock(
            "--- start ",
            "--- end "
          ) { start, end, blockId, blockText ->
            val holder = captureMap.idMap[blockId]!!

            holder.start = start
            holder.result = holder.command.processConsole?.invoke(con, blockText)
            holder
          }

          // if there was a previous capture started, append to it
          if(currentCapture.get() != null){
            val holder = currentCapture.get()!!

            //if no new holders, append all. Else: append before the start of the first holder
            if(newHolders.isEmpty()) {
              holder.text = con.stdout.subSequence(holder.start, con.stdout.length)
            } else {
              holder.text = con.stdout.subSequence(holder.start, newHolders[0].start - "--- end ".length)
            }

            holder.command.processConsole?.invoke(con, con.newIn)
          }

          // if found new blocks, change the current capture
          if(!newHolders.isEmpty()) {
            currentCapture.set(newHolders.last())
          }


        }
      ),
      timeoutMs = timeoutMs
    )

    return ScriptCommandResult(captureMap, x)
  }

  private fun captureProcessing(console: Console) {
    console.stdout.mapEachNamedTextBlock(
      "--- start ",
      "--- end "
    ) { start, end, blockId, blockText ->
      val holder = captureMap.idMap[blockId]!!

//      holder.result = holder.command?.processConsole(console, blockText)
      holder.text = blockText.trim()
    }
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

      val blockEndFullname = "$blockEnd$blockName"

      val blockEndPos = this.indexOf(blockEndFullname, pos + blockStart.length)

      if (blockEndPos == -1) {
        list += block(pos, -1, blockName, this.subSequence(pos, length))
        break
      } else {
        // doesn't make much sense, so fuck it
        if (blockEndPos >= end) break

        list += block(pos, blockEndPos, blockName, this.subSequence(blockNameEnd + 1, blockEndPos))

        pos = blockEndPos + blockEnd.length
      }
    }

    return list
  }

}