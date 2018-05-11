package fast.log

import fast.log.LogLevel.*
import fast.log.MessageFilterType.*
import java.util.ArrayList


open class LoggerImpl<BC, O>(
  @get:JvmName("_name")
  val name: String,
  var classifier: BC? = null
) {
   val chains = ArrayList<MessageChain<BC, O>>()

  lateinit var level: LogLevel

  val simpleName = name.substringAfterLast('.').substringBeforeLast("\$Companion")

  fun withBase(classifier: BC): LoggerImpl<BC, O> {
    this.classifier = classifier
    return this
  }

  override fun toString(): String {
    return "LoggerImpl($name, $level, chains=${chains.size}"
  }

  fun setIfHigher(level: LogLevel) {
    if(this.level < level) this.level = level
  }

  inline fun trace(lazyMsg: () -> O) {
    if (isTraceEnabled()) log(TRACE, lazyMsg = lazyMsg)
  }

  inline fun debug(lazyMsg: () -> O) {
    if (isDebugEnabled()) log(DEBUG, lazyMsg = lazyMsg)
  }

  inline fun info(lazyMsg: () -> O) {
    if (isInfoEnabled()) log(INFO, lazyMsg = lazyMsg)
  }

  inline fun warn(lazyMsg: () -> O) {
    if (isWarnEnabled()) log(WARN, lazyMsg = lazyMsg)
  }

  inline fun error(lazyMsg: () -> O) {
    if (isWarnEnabled()) log(ERROR, lazyMsg = lazyMsg)
  }

  inline fun warn(e: Throwable, lazyMsg: () -> O)  = if (isWarnEnabled()) log(WARN, e = e, lazyMsg = lazyMsg) else {}

  inline fun <C> trace(classifier: C?, lazyMsg: () -> O)  = if (isTraceEnabled()) log(TRACE, msgClassifier = classifier, lazyMsg = lazyMsg) else {}
  inline fun <C> debug(classifier: C?, lazyMsg: () -> O)  = if (isDebugEnabled()) log(DEBUG, msgClassifier = classifier, lazyMsg = lazyMsg) else {}
  inline fun <C> info(classifier: C?, lazyMsg: () -> O)  = if (isInfoEnabled()) log(INFO, msgClassifier = classifier, lazyMsg = lazyMsg) else {}
  inline fun <C> warn(classifier: C?, lazyMsg: () -> O)  = if (isWarnEnabled()) log(WARN, msgClassifier = classifier, lazyMsg = lazyMsg) else {}
  inline fun <C> error(classifier: C?, lazyMsg: () -> O)  = if (isErrorEnabled()) log(ERROR, msgClassifier = classifier, lazyMsg = lazyMsg) else {}

  inline fun log(level: LogLevel, msgClassifier: Any? = null, e: Throwable? = null, args: Array<out Any>? = null, lazyMsg: () -> O): Unit {
    for (chain in chains) {
      chain.log(level, msgClassifier, e, args, lazyMsg)
    }
  }

  fun log(level: LogLevel, _obj: O, msgClassifier: Any? = null, e: Throwable? = null, vararg args: Any?): Unit {
    for (chain in chains) {
      chain.log(level, _obj, msgClassifier, e, args)
    }
  }

  // Got access exception with a plain inner class
  class MessageChain<BC, O>(val logger:  LoggerImpl<BC, O>) {
    val filters = ArrayList<MessageFilter<*, *>>()
    val appenders = ArrayList<Appender<Any, Any>>()
    var transformer: Transformer<Any, O>? = null


    //  @SuppressWarnings("UNCHECKED_CAST")
    inline fun log(level: LogLevel, msgClassifier: Any? = null, e: Throwable? = null, args: Array<out Any>? = null, lazyMsg: () -> O): Unit {
      if (!logger.isEnabled(level)) return

      for (_filter in filters) {
        val filter = _filter as MessageFilter<Any, O>

        when (filter.type) {
          simple -> if (!filter.accept(msgClassifier, level)) return
          simpleWithArgs -> if (args != null && !filter.accept(msgClassifier, level, args)) return
          else -> {/* ignore */
          }
        }
      }

      val obj = lazyMsg()

      log(level, obj, msgClassifier, e, args)
    }

    fun log(level: LogLevel, _obj: O, msgClassifier: Any? = null, e: Throwable? = null, vararg args: Any?): Unit {
      if (!logger.isEnabled(level)) return

      if (e != null) {
        //TODO push it into transformer
        e.printStackTrace()

        return
      }

      val obj: O = if (_obj is CharSequence && _obj.contains("{}")) {
        _obj.replaceSlf4jPlaceHolders(args)
      } else {
        _obj
      } as O

      for (_filter in filters) {
        val filter = _filter as MessageFilter<Any, O>

        when (filter.type) {
          obj -> if (!filter.accept(msgClassifier, obj, level)) return
          objWithArgs -> if (args != null && !filter.accept(msgClassifier, obj, level, args)) return
          else -> {/* ignore */
          }
        }
      }

      if (transformer == null) {
        for (appender in appenders) {
          appender.append(obj as Any, msgClassifier, level)
        }
      } else {
        var transformed: Any? = null

        for (appender in appenders) {
          if (!appender.supportsTransform()) {
            if (transformed == null) transformed = transformer?.transform(msgClassifier, obj, level) ?: obj as Any
            appender.append(transformed, msgClassifier, level)
          } else {
            appender.transform(transformer!! as Transformer<Any, Any>, msgClassifier, obj as Any, level, logger as LoggerImpl<Any?, Any>, e, args)
          }
        }
      }
    }
  }

  fun <C> trace(classifier: C, obj: O) =  log(TRACE, msgClassifier = classifier, _obj = obj)
  fun <C> debug(classifier: C, obj: O) =  log(DEBUG, msgClassifier = classifier, _obj = obj)
  fun <C> info(classifier: C, obj: O) =  log(INFO, msgClassifier = classifier, _obj = obj)
  fun <C> warn(classifier: C, obj: O) =  log(WARN, msgClassifier = classifier, _obj = obj)
  fun <C> error(classifier: C, obj: O) =  log(ERROR, msgClassifier = classifier, _obj = obj)

  fun isInfoEnabled() = isEnabled(INFO)
  fun isTraceEnabled() = isEnabled(TRACE)
  fun isDebugEnabled() = isEnabled(DEBUG)
  fun isWarnEnabled() = isEnabled(WARN)
  fun isErrorEnabled() = isEnabled(ERROR)

  fun isEnabled(levelToCheck: LogLevel) = level.ordinal <= levelToCheck.ordinal
}

private fun CharSequence.replaceSlf4jPlaceHolders(args: Array<out Any?>): StringBuilder {
  val sb = StringBuilder()

  var prevPos = 0
  var pos = 0
  var argIndex = 0

  while (argIndex < args.size && pos < length) {
    val pIndex = indexOf("{}", pos)

    if (pIndex == -1) {
      // no more place holders -> append the rest of the string
      sb.append(subSequence(pos, length))
      break
    } else {
      // append prefix, arg and advance position after placeholder
      sb.append(subSequence(pos, pIndex))
      sb.append(args[argIndex++])
      pos = pIndex + 2
    }
  }

  return sb
}
