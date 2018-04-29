package fast.dsl

import fast.inventory.Host
import fast.ssh.KnownHostsConfig

class SshDSL {
  val configs = LinkedHashMap<String, (Host) -> KnownHostsConfig>()

  infix fun String.with(block: (Host) -> KnownHostsConfig) {
    configs[this] = block
  }

  fun privateKey(host: Host, user: String = "root", block: (KnownHostsConfig.() -> Unit)? = null): KnownHostsConfig {
    val config = KnownHostsConfig(address = host.address, authUser = user)

    return (if (block != null) config.apply(block) else config)
  }

  fun password(
    host: Host, user: String, password: String,
    block: (KnownHostsConfig.() -> Unit)? = null
  ): KnownHostsConfig {
    val config = KnownHostsConfig(address = host.address, authUser = user, authPassword = password)

    return (if (block != null) config.apply(block) else config)
  }

  fun forHost(host: Host): KnownHostsConfig {
    /*for (group in host.groups) {
      val myConfig  = configs[group.name]
    }*/

    val configLambda = {
      // match by a group name
      val myGroup = host.groups.find { configs[it.name] != null }

      when {
        myGroup != null -> configs[myGroup.name]!!

      // match by a host name
        configs[host.name] != null -> configs[host.name]!!

      // check 'other'
        else -> configs["other"]
          ?: throw Exception("none of the hosts matched ssh configuration and 'other' ssh group is missing")
      }
    }()

    return configLambda(host)
  }
}