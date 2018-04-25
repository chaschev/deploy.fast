package fast.ssh

class KnownHostsConfig(
  var knownHostsPath: String = "${System.getenv("HOME")}/.ssh/known_hosts",
  var keyPath: String? = null,
  var keyPassword: String? = null,

  address: String,
  authUser: String,
  authPassword: String? = null,
  port: Int = 22
)  : SshConfig(address, authUser, authPassword) {
}