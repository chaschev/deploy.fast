package fast.dsl

class Hosts(
  val hosts: List<String>
)

class Host(
  val address: String,
  val name: String = address,
  internal val groups: ArrayList<Group>
)

interface IGroup {
  val name: String
  val hosts: List<Host>
}

class CompositeGroup() : IGroup {
  override val name: String by lazy { TODO() }
  override val hosts: List<Host> by lazy { TODO() }
}

class Group(
  override val name: String,
  override val hosts: List<Host>
) : IGroup {
  internal fun postCreate() {
    for (host in hosts) {
      host.groups += this
    }
  }
}