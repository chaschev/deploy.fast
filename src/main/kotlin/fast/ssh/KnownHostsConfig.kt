package fast.ssh

class KnownHostsConfig(
    val path: String,
    override val address: String,
    override val authUser: String,
    override val authPassword: String? = null,
    val keyPassword: String? = null)

    : SshConfig(address, authUser, authPassword) {
}