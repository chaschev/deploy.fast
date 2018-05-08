package fast.ssh

import fast.inventory.Host

open class SshConfig(
  open val host: Host,
  open val authUser: String,
  open val authPassword: String? = null,
  open val port: Int = 22
) {
  val address: String
    get() = host.address
}

