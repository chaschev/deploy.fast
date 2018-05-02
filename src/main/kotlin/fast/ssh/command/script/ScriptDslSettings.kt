package fast.ssh.command.script

import fast.ssh.process.Console

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