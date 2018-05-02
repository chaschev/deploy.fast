package fast.ssh.command.script

import fast.api.UserRights

class AddRightsCommandDsl(
  var files: List<String>
): ScriptDslSettings(), ScriptLines {
  var create: Boolean = false
  var recursive: Boolean = true

  lateinit var rights: UserRights

  override fun lines(): List<String> {
    val lines = ArrayList<String>()

    val filesStr = files.joinToString(" ")

    if(create) {
      lines += "mkdir -p $filesStr"
    }

    lines += "chown ${if(recursive) "-R" else ""} ${rights.owner.name}:${rights.owner.group} $filesStr"
    lines += "chmod ${if(recursive) "-R" else ""} ${rights.access} $filesStr "

    return lines;
  }
}