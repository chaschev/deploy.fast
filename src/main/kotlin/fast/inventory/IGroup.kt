package fast.inventory

interface IGroup : IWithVars {
  val name: String
  val hosts: List<Host>

  fun findHost(predicate: (Host) -> Boolean) = hosts.find(predicate)

  fun getHostsForName(name: String): List<Host>
}