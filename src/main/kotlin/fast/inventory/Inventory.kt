package fast.inventory

import java.util.concurrent.ConcurrentHashMap

class Inventory(
  val groups: List<IGroup>
) {
  val group = CompositeGroup().groups.addAll(groups)
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
}

class CompositeGroup() : IGroup {
  val groups = ArrayList<IGroup>()

  override val name: String by lazy { TODO() }
  override val hosts: List<Host> by lazy { TODO() }
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