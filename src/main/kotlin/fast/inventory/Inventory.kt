package fast.inventory

import fast.runtime.DeployFast
import fast.ssh.KnownHostsConfig
import org.kodein.di.generic.instance
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Inventory(
  val groups: List<IGroup>
) {
  val asOneGroup = CompositeGroup("inventory")

  init {
    asOneGroup.groups.addAll(groups)
  }

  val hosts
    get() = asOneGroup.hosts

  val props = ConfigProps(File(".fast/fast.props"))


  val vars = ConcurrentHashMap<String, Any>()

  private var initialised = false

  lateinit var sshConfig: KnownHostsConfig

  val activeHosts by lazy {
    DeployFast.FASTD.instance(tag = "runAtHosts") as List<Host>
  }

  fun group(name: String): IGroup {
    return (
      if(name.contains('.'))
        groupPath(name.split(Regex("\\.")))
      else
        null
      ) ?:
       asOneGroup.findGroup { it.name == name } ?: throw Exception("group not found: $name")
  }

  private fun groupPath(path: List<String>, parent: IGroup = asOneGroup): IGroup? {
    if(path.isEmpty()) return parent

    return if(parent is CompositeGroup) {
      val pathEntry = parent.groups.find { it.name == path.first() } ?: return null

      groupPath(path.subList(1, path.size), pathEntry)
    } else {
      null
    }
  }

  fun group(predicate: (IGroup) -> Boolean) = asOneGroup.findGroup(predicate) ?: throw Exception("group not found by criteria")

  operator fun get(name: String) = group(name)

  fun initHosts(): Inventory {
    asOneGroup.forEachGroup { group ->
      group.hosts.forEach { it._groups += group }
    }

    initialised = true

    return this
  }

  fun initialised() = initialised

  fun getVar(name: String, host: Host): Any? {
    if(host._getVar(name) != null) return host._getVar(name)

    val groupOverrideValue = host.groups.find { it._getVar(name) != null }?._getVar(name)

    if(groupOverrideValue != null)  return groupOverrideValue

    return props[name] ?: vars[name]
  }

  fun getHostsForName(name: String) = asOneGroup.getHostsForName(name)
}