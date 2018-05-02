package fast.ssh.command.script

interface ScriptItem

interface ScriptBlock : ScriptItem {
  fun getString(): String
}

interface ScriptLines: ScriptItem {
  fun lines(): List<String>
}

