package fast.inventory

import java.util.concurrent.ConcurrentHashMap

class Inventory(
  val groups: List<IGroup>
) {
  val asOneGroup = CompositeGroup("inventory")

  private var initialised = false



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
}

data class Host(
  val address: String,
  val name: String = address,
  val vars: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
) {
  internal val _groups: ArrayList<Group> = ArrayList()

  val groups: List<Group> = _groups
}

interface IGroup {
  val name: String
  val hosts: List<Host>

  fun findHost(predicate: (Host) -> Boolean) = hosts.find(predicate)

  fun getHostsForName(name: String): List<Host>
}

class CompositeGroup(
  override val name: String
) : IGroup {
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
) : IGroup {
  override fun getHostsForName(name: String): List<Host> {
    if(this.name == name) return hosts

    val myHost = hosts.find { it.name == name }

    return listOfNotNull(myHost)
  }
}