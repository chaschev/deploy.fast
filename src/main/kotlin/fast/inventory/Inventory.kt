package fast.inventory

import fast.lang.InitLater
import fast.runtime.DeployFastDI.FAST
import fast.ssh.KnownHostsConfig
import org.kodein.di.generic.instance
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Inventory(
  val groups: List<IGroup>
) {
  val asOneGroup = CompositeGroup("inventory")

  val props = ConfigProps(File(".fast/fast.props"))

  val vars = ConcurrentHashMap<String, Any>()

  private var initialised = false

  lateinit var sshConfig: KnownHostsConfig

  init {
    asOneGroup.groups.addAll(groups)
  }

  fun group(name: String) = asOneGroup.findGroup { it.name == name } ?: throw Exception("group not found: $name")
  fun group(predicate: (IGroup) -> Boolean) = asOneGroup.findGroup(predicate) ?: throw Exception("group not found by criteria")

  operator fun get(name: String) = group(name)

  fun init(): Inventory {
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

    return props[name] ?:
      vars[name]
  }
}

interface IWithVars {
  fun _getVar(name: String): Any?
  fun setVar(name: String, value: Any)
}

open class WithVars : IWithVars {
  private var _vars: ConcurrentHashMap<String, Any>? = null

  override fun _getVar(name: String): Any? {
    if(_vars == null) return null
    return _vars!![name]
  }

  override fun setVar(name: String, value: Any) {
    if(_vars == null) _vars = ConcurrentHashMap()
    _vars!![name] = value
  }

}

data class Host(
  val address: String,
  val name: String = address
) : WithVars() {
  internal val _groups: ArrayList<Group> = ArrayList()

  val inventory: Inventory by FAST.instance()

  val groups: List<Group> = _groups

  fun getVar(name: String) = inventory.getVar(name, this)
}

interface IGroup : IWithVars {
  val name: String
  val hosts: List<Host>

  fun findHost(predicate: (Host) -> Boolean) = hosts.find(predicate)

  fun getHostsForName(name: String): List<Host>
}

class CompositeGroup(
  override val name: String
) : IGroup, WithVars() {
  val groups = ArrayList<IGroup>()

  override val hosts: List<Host> by lazy {
    groups.flatMapTo(ArrayList()) {it.hosts}
  }

  fun findGroup(predicate: (IGroup) -> Boolean): IGroup? {
    val group = groups.find(predicate)

    if(group != null) return group

    for (group in groups.filterIsInstance(CompositeGroup::class.java)) {
      val found = group.findGroup(predicate)

      if(found != null) return found
    }

    return null
  }

  fun forEachGroup(block: (Group) -> Unit) {
    for (group in groups) {
      if(group is Group)
        block(group)
      else if(group is CompositeGroup)
        group.forEachGroup(block)
    }
  }

  override fun getHostsForName(name: String): List<Host> {
    if(this.name == name) return hosts

    val myGroup = findGroup { it.name == name }

    if(myGroup != null) return myGroup.hosts

    val myHost = hosts.find { it.name == name }

    return listOfNotNull(myHost)
  }

}

data class Group(
  override val name: String,
  override val hosts: List<Host>
) : IGroup, WithVars() {
  override fun getHostsForName(name: String): List<Host> {
    if(this.name == name) return hosts

    val myHost = hosts.find { it.name == name }

    return listOfNotNull(myHost)
  }
}