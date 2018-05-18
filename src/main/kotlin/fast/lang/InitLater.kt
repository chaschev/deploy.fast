package fast.lang

import kotlin.reflect.KProperty

class InitLater(private val finalize: Boolean = true) {
  private var value: Any? = null

  operator fun <T> getValue(obj: Any, property: KProperty<*>): T {
    if(value == null) throw UninitializedPropertyAccessException("property $property is not initialized")
    return value!! as T
  }

  operator fun <T> setValue(obj: Any, property: KProperty<*>, value: T) {
    if(finalize) {
      require(this.value == null, {"value is already set for property $property"})
    }

    this.value = value
  }
}

class LazyVar<T>(private val initBlock:() -> T) {
  private var value: T? = null

  operator fun getValue(obj: Any, property: KProperty<*>): T {
    val t = value

    if(t == null) {
      val x = initBlock()
      value = x
      return x
    }

    return t
  }

  operator fun setValue(obj: Any, property: KProperty<*>, value: T) {
    this.value = value
  }
}

fun <T> lazyVar(initBlock:() -> T) = LazyVar(initBlock)
fun initLater(finalize: Boolean = true) = InitLater(finalize)
