package fast.inventory

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