package fast.log

import fast.log.MessageFilterType.*
import java.util.ArrayList


open class LoggerImpl<BC, O>(
  @get:JvmName("_name")
  val name: String,
  val classifier: BC? = null
) {
  inline fun info(lazyMsg: () -> O) {
    if (isInfoEnabled()) log(LogLevel.info, lazyMsg =  lazyMsg)
  }

  //  @SuppressWarnings("UNCHECKED_CAST")
  inline fun log(level: LogLevel, classifier: BC? = null, e: Throwable? = null, args: Array<out Any>? = null, lazyMsg: () -> O) {
    if (!isEnabled(level)) return

    for (_filter in filters) {
      val filter = _filter as MessageFilter<Any, O>

      when(filter.type) {
        simple -> if (!filter.accept(classifier, level)) return
        simpleWithArgs ->  if ( args != null && !filter.accept(classifier, level, args)) return
        else -> {/* ignore */}
      }
    }

    val obj = lazyMsg()

    log(level, obj, classifier, e, args)
  }

  fun log(level: LogLevel, _obj: O, classifier: BC? = null, e: Throwable? = null, vararg args: Any?) {
    if(e != null ) {
      //TODO push it into transformer
      e.printStackTrace()

      return
    }

    val obj: O = if(_obj is CharSequence && _obj.contains("{}")) {
      _obj.replaceSlf4jPlaceHolders(args)
    } else {
      _obj
    } as O

    for (_filter in filters) {
      val filter = _filter as MessageFilter<Any, O>

      when(filter.type) {
        obj -> if (!filter.accept(classifier,obj, level)) return
        objWithArgs ->  if ( args != null && !filter.accept(classifier, obj, level, args)) return
        else -> {/* ignore */}
      }
    }

    if (transformer == null) {
      for (appender in appenders) {
        appender.append(obj as Any, classifier, level)
      }
    } else {
      var transformed: Any? = null

      for (appender in appenders) {
        if (!appender.supportsTransform()) {
          if (transformed == null) transformed = transformer?.transform(classifier, obj, level) ?: obj as Any
          appender.append(transformed, classifier, level)
        } else {
          appender.transform(transformer!! as Transformer<Any, Any>, classifier, obj as Any, level, this as LoggerImpl<Any?, Any>, e, args)
        }
      }
    }
  }

  fun isInfoEnabled(): Boolean {
    return isEnabled(LogLevel.info)
  }

  fun isEnabled(levelToCheck: LogLevel) = level.ordinal <= levelToCheck.ordinal

  val filters = ArrayList<MessageFilter<*, *>>()
  val appenders = ArrayList<Appender<Any, Any>>()
  var transformer: Transformer<Any, O>? = null

  lateinit var level: LogLevel

  val simpleName = name.substringAfterLast('.')

  override fun toString(): String {
    return "LoggerImpl($name, $level, filters=$filters, appenders=$appenders, transformer=$transformer"
  }
}

private fun  CharSequence.replaceSlf4jPlaceHolders(args: Array<out Any?>): StringBuilder {
  val sb = StringBuilder()

  var prevPos = 0
  var pos = 0
  var argIndex = 0

  while(argIndex < args.size && pos < length) {
    val pIndex = indexOf("{}", pos)

    if(pIndex == -1) {
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
