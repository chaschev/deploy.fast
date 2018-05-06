package fast.lang

import kotlin.reflect.KProperty

class InitLater(val finalize: Boolean = true) {
  private var value: Any? = null

  operator fun <T> getValue(obj: Any, property: KProperty<*>): T {
    if(value == null) throw Exception("property $property is not initialized")
    return value!! as T
  }

  operator fun <T> setValue(obj: Any, property: KProperty<*>, value: T) {
    if(finalize) {
      require(this.value == null, {"value is already set for property $property"})
    }

    this.value = value
  }

  companion object {
    fun initLater(finalize: Boolean = true) = InitLater(finalize)
  }
}