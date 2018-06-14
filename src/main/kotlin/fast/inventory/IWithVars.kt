package fast.inventory

interface IWithVars {
  fun _getVar(name: String): Any?
  fun setVar(name: String, value: Any)

  fun getVars(): Map<String, Any>
}