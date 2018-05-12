package fast.log

enum class MessageFilterType {
  simple, obj, objWithArgs, simpleWithArgs
}

interface MessageFilter<C, O> {
  val type: MessageFilterType
  val name: String

  //simple
  fun accept(classifier: C?, level: LogLevel): Boolean = TODO()

  //simpleWithArgs
  fun accept(classifier: C?, level: LogLevel, vararg args: Any): Boolean = accept(classifier, level)

  //obj
  fun accept(classifier: C?, obj: O, level: LogLevel): Boolean = accept(classifier, level)

  //objWithArgs
  fun accept(classifier: C?, obj: O, level: LogLevel, vararg args: Any): Boolean = accept(classifier, level)

  companion object {
    inline fun <C> of(crossinline filter: (C?, LogLevel) -> Boolean) =
      object : MessageFilter<C, Any> {
        override val name: String
          get() = "filter(cls, level)"

        override val type: MessageFilterType
          get() = MessageFilterType.simple

        override fun accept(classifier: C?, level: LogLevel): Boolean {
          return filter(classifier, level)
        }
      }

    inline fun of(crossinline filter: (LogLevel) -> Boolean) =
      object : MessageFilter<Any, Any> {
        override val name: String
          get() = "filter(level)"

        override val type: MessageFilterType
          get() = MessageFilterType.simple

        override fun accept(classifier: Any?, level: LogLevel): Boolean {
          return filter(level)
        }
      }


    fun <C, O> ofObj(filter: (C?, O, LogLevel) -> Boolean) =
      object : MessageFilter<C, O> {
        override val name: String
          get() = "filter(cls, obj, level)"

        override val type: MessageFilterType
          get() = MessageFilterType.obj

        override fun accept(classifier: C?, level: LogLevel): Boolean {
          TODO("not supported")
        }

        override fun accept(classifier: C?, obj: O, level: LogLevel): Boolean {
          return filter(classifier, obj, level)
        }
      }


    fun <C, O> ofObj(filter: (C?, O) -> Boolean) =
      object : MessageFilter<C, O> {
        override val name: String
          get() = "filter(cls, obj)"

        override val type: MessageFilterType
          get() = MessageFilterType.obj

        override fun accept(classifier: C?, level: LogLevel): Boolean {
          TODO("not supported")
        }

        override fun accept(classifier: C?, obj: O, level: LogLevel): Boolean {
          return filter(classifier, obj)
        }
      }

    fun <O> ofObj(filter: (O) -> Boolean) =
      object : MessageFilter<Any, O> {
        override val name: String
          get() = "filter(obj)"


        override val type: MessageFilterType
          get() = MessageFilterType.obj

        override fun accept(classifier: Any?, level: LogLevel): Boolean {
          TODO("not supported")
        }

        override fun accept(classifier: Any?, obj: O, level: LogLevel): Boolean {
          return filter(obj)
        }
      }

    fun <O> ofArgs(filter: (Any?, O, LogLevel, args: Array<out Any>) -> Boolean) =
      object : MessageFilter<Any, O> {
        override val name: String
          get() = "filter(cls, obj, level, args)"

        override val type: MessageFilterType
          get() = MessageFilterType.objWithArgs

        override fun accept(classifier: Any?, obj: O, level: LogLevel, vararg args: Any): Boolean {
          return filter(classifier, obj, level, args)
        }
      }

  }
}