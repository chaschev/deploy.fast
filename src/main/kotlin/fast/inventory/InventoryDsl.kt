package fast.inventory

import fast.sample.DeployEnvironment

class InventoryDsl {
  private val groups = ArrayList<GroupDsl>()

  fun group(name: String, block: GroupDsl.() -> Unit) {
    groups += GroupDsl(name).apply(block)
  }

  fun withGroups(vararg names: String, block: GroupDsl.() -> Unit) {
    groups.filter { names.contains(it.name) }.forEach(block)
  }


  fun group(env: DeployEnvironment, block: GroupDsl.() -> Unit) {
    group(env.name, block)
  }

  companion object {
    fun inventory(block: InventoryDsl.() -> Unit): Inventory {
      return InventoryDsl().apply(block).build()
    }
  }

  fun build(): Inventory {

    val simpleGroups: Map<String, Group> = groups
      .filter { it.subgroups.isEmpty() }
      .map {
        val group = Group(it.name, it.hosts)

        for ((k,v) in it.vars.orEmpty()) {
          group.setVar(k, v)
        }

        group.name to group
    }.toMap()

    val compositeGroups = HashMap<String, CompositeGroup>()

    groups
      .filter { it.subgroups.isNotEmpty() }
      .forEach { compositeGroups[it.name] = CompositeGroup(it.name) }

    groups
      .filter { it.subgroups.isNotEmpty() }
      .forEach {
        val compo = compositeGroups[it.name]!!
        for (groupName in it.subgroups) {
          val group : IGroup = (simpleGroups[groupName] ?: compositeGroups[groupName]) as IGroup? ?: throw Exception("group not found: $groupName")

          compo.groups += group
        }
      }

    return Inventory(
      listOf(compositeGroups.values.toList() as List<IGroup>, simpleGroups.values.toList()).flatten()
    ).initHosts()

  }
}