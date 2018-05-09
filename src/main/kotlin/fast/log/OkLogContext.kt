package fast.log

import fast.lang.initLater
import java.util.concurrent.ConcurrentHashMap

class OkLogContext(
  val _dsl: LoggerFactoryDSL.() -> Unit
) {
  private val appenderMap = ConcurrentHashMap<String, Appender<*, *>>()

  var debugMode = false

  fun getAppender(name: String, block: () -> Appender<*, *>) {
    if (debugMode) println("getAppender($name)")

    appenderMap.getOrPut(name, block)
  }

  fun getLogger(name: String): LoggerImpl<Any, Any> {
    return getClassifiedLogger(name, null)
  }

  fun <C> getClassifiedLogger(name: String, classifier: C?): LoggerImpl<Any, Any> {
    if (debugMode) println("getLogger($name, $classifier)")

    val logger = LoggerImpl<Any, Any>(name, classifier)
    val dsl = LoggerFactoryDSL(logger, this)

    logger.level = dsl.defaultLevel

    dsl.apply(_dsl)

    return logger
  }

  companion object {
    var okLog: OkLogContext by initLater(finalize = true)
  }
}