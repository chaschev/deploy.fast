package fast.inventory

class GroupDsl(val name: String) {
  val hosts = ArrayList<Host>()
  val subgroups = HashSet<String>()
  var vars: HashMap<String, String>? = null

  //todo remove
  var modifyHosts: (Host.() -> Unit)? = null

  fun hosts(vararg hosts: Host) {
    this.hosts.addAll(hosts)
  }

  fun subgroup(name: String) {
    subgroups += name
  }

  fun vars(vararg vars: Pair<String, String>) {
    if(this.vars == null) this.vars = HashMap()

    this.vars!! += vars
  }

  fun modifyHosts(block: Host.() -> Unit) {
    this.modifyHosts = block
    hosts.forEach(block)
  }
}