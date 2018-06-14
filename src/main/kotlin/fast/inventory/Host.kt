package fast.inventory

import fast.runtime.DeployFastDI
import org.kodein.di.generic.instance
import org.slf4j.MarkerFactory

data class Host(
  val address: String,
  val name: String = address
) : WithVars() {
  internal val _groups: ArrayList<Group> = ArrayList()

  val inventory: Inventory by DeployFastDI.FAST.instance()

  val groups: List<Group> = _groups

  fun getVar(name: String) = inventory.getVar(name, this)

  companion object {
    val IPS_OUT = MarkerFactory.getMarker("IPS_OUT")
    val local = Host("localhost")
  }
}