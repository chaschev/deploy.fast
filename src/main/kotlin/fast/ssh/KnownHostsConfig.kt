package fast.ssh

class KnownHostsConfig(
  val knownHostsPath: String,
  val keyPath: String? = null,
  val keyPassword: String? = null,

  override val address: String,
  override val authUser: String,
  override val authPassword: String? = null
)

    : SshConfig(address, authUser, authPassword) {
}