package fast.lang

import kotlin.reflect.KProperty

class DynamicVar<T>(private var lazyInit: () -> T, val cache: Boolean = true) {
  @Volatile
  var cachedValue: T? = null

  operator fun  getValue(obj: Any, property: KProperty<*>): T {
    if(cache) {
      val v = cachedValue

      if(v == null) {
        val x = lazyInit()
        cachedValue = x
        return x
      }

      return v
    }

  }

  operator fun setValue(obj: Any, property: KProperty<*>, lazyInit: () -> T) {
    if(cache) {
      cachedValue = null
    }

    this.lazyInit = lazyInit
  }

  companion object {
    fun <T> dynamic(finalize: Boolean = true, lazyInit: () -> T) =
      DynamicVar(lazyInit, finalize)

  }
}