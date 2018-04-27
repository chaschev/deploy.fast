package fast.ssh

import mu.KLogging
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.*


class GenericSshProvider(
  val config: SshConfig)
  : SshProvider {

  companion object : KLogging()

  internal lateinit var sshClient: SSHClient

  override fun connect(): SshProvider {
    if (config is KnownHostsConfig) {
      try {
        val ssh = SSHClient()

        logger.info { "connecting to ${config.address}" }

        ssh.loadKnownHosts(File(config.knownHostsPath))

        val keyProvider = if (config.keyPath != null) {
          if (config.keyPassword == null)
            ssh.loadKeys(config.keyPath)
          else
            ssh.loadKeys(config.keyPath, config.keyPassword)
        } else null

        ssh.addHostKeyVerifier(PromiscuousVerifier())

        ssh.connect(config.address)

        logger.info { "connected" }

        val location = "${System.getenv("HOME")}/.vagrant.d/insecure_private_key"

        if (config.authPassword == null) {
          if(keyProvider == null)
            ssh.authPublickey(config.authUser)
          else
            ssh.authPublickey(config.authUser, location)
        } else {
          ssh.authPassword(config.authUser, config.authPassword)
        }

        logger.info { "authenticated" }

        sshClient = ssh
        //ssh.authPassword(sshAddress.authUser, sshAddress.authPassword)
      } catch (e: TransportException) {
        throw e
      }
    } else {
      TODO()
    }

    return this
  }

  override fun createSession(): GenericSshSession = GenericSshSession(this)

  override fun withSession(block: (session: GenericSshSession) -> Unit) =
    createSession().use(block)

  override fun close() = sshClient.close()
}
