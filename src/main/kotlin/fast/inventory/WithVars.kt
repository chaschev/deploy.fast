package fast.inventory

import java.util.concurrent.ConcurrentHashMap

open class WithVars : IWithVars {
  override fun getVars(): Map<String, Any> {
    TODO("not implemented")
  }

  private var _vars: ConcurrentHashMap<String, Any>? = null

  override fun _getVar(name: String): Any? {
    if(_vars == null) return null
    return _vars!![name]
  }

  override fun setVar(name: String, value: Any) {
    if(_vars == null) _vars = ConcurrentHashMap()
    _vars!![name] = value
  }
}