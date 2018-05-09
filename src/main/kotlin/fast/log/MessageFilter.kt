package fast.log

interface MessageFilter<C, O> {
  fun accept(obj: O, level: LogLevel): Boolean
  fun accept(classifier: C?, obj: O, level: LogLevel): Boolean = accept(obj, level)

  companion object {
    fun <C, O> of(filter: (C?, O, LogLevel) -> Boolean) =
      object : MessageFilter<C, O> {
        override fun accept(obj: O, level: LogLevel): Boolean {
          TODO("not supported")
        }

        override fun accept(classifier: C?, obj: O, level: LogLevel): Boolean {
          return filter(classifier, obj, level)
        }
      }


    fun <C, O> of(filter: (C?, O) -> Boolean) =
      object : MessageFilter<C, O> {
        override fun accept(obj: O, level: LogLevel): Boolean {
          TODO("not supported")
        }

        override fun accept(classifier: C?, obj: O, level: LogLevel): Boolean {
          return filter(classifier, obj)
        }
      }

    fun <O> of(filter: (O) -> Boolean) =
      object : MessageFilter<Any, O> {
        override fun accept(obj: O, level: LogLevel): Boolean {
          return filter(obj)
        }
      }
  }
}