package fast.inventory

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