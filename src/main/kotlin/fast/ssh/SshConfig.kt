package fast.ssh

open class SshConfig(
    open val address: String,
    open val authUser: String,
    open val authPassword: String? = null
)

