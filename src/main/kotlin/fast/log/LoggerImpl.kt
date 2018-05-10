package fast.log

import java.util.ArrayList

class LoggerImpl<BC, O>(
  val name: String,
  val classifier: BC? = null
) {
  inline fun info(lazyMsg: () -> O) {
    if (isInfoEnabled()) log(LogLevel.info, null, lazyMsg)
  }

  //  @SuppressWarnings("UNCHECKED_CAST")
  inline fun log(level: LogLevel, classifier: BC? = null, lazyMsg: () -> O) {
    if (!isEnabled(level)) return

    val obj = lazyMsg()

    for (_filter in filters) {
      val filter = _filter as MessageFilter<Any, O>

      if (!filter.accept(classifier, obj, level)) {
        return
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
          appender.transform(transformer!! as Transformer<Any, Any>, classifier, obj as Any, level)
        }
      }
    }

  }

  fun isInfoEnabled(): Boolean {
    return isEnabled(LogLevel.info)
  }

  public fun isEnabled(levelToCheck: LogLevel) = level.ordinal <= levelToCheck.ordinal

  val filters = ArrayList<MessageFilter<*, *>>()
  val appenders = ArrayList<Appender<Any, Any>>()
  var transformer: Transformer<Any, O>? = null

  lateinit var level: LogLevel
}