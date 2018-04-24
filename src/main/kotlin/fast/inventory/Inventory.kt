package fast.inventory

import java.util.concurrent.ConcurrentHashMap

class Inventory(
  val groups: List<IGroup>
) {
  val asOneGroup = CompositeGroup()

  fun group(name: String) = asOneGroup.findGroup { it.name == name } ?: throw Exception("group not found: $name")
  fun group(predicate: (IGroup) -> Boolean) = asOneGroup.findGroup(predicate) ?: throw Exception("group not found by criteria")

  operator fun get(name: String) = group(name)

  init {
    asOneGroup.groups.addAll(groups)
  }
}

data class Host(
  val address: String,
  val name: String = address,
  internal val groups: ArrayList<Group> = ArrayList(),
  val vars: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
)

interface IGroup {
  val name: String
  val hosts: List<Host>

  fun findHost(predicate: (Host) -> Boolean) = hosts.find(predicate)
}

class CompositeGroup() : IGroup {
   val groups = ArrayList<IGroup>()

  override val name: String by lazy { TODO() }
  override val hosts: List<Host> by lazy { TODO() }

  fun findGroup(predicate: (IGroup) -> Boolean): IGroup? {
    val group = groups.find(predicate)

    if(group != null) return group

    for (group in groups.filterIsInstance(CompositeGroup::class.java)) {
      val found = group.findGroup(predicate)

      if(found != null) return found
    }

    return null
  }
}

data class Group(
  override val name: String,
  override val hosts: List<Host>
) : IGroup {
  internal fun postCreate() {
    for (host in hosts) {
      host.groups += this
    }
  }
}