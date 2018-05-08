package fast.ssh

import fast.inventory.Host

class KnownHostsConfig(
  var knownHostsPath: String = "${System.getenv("HOME")}/.ssh/known_hosts",
  var keyPath: String? = null,
  var keyPassword: String? = null,

  host: Host,
  authUser: String,
  authPassword: String? = null,
  port: Int = 22
)  : SshConfig(host, authUser, authPassword) {
}