package fast.log

import fast.inventory.Host
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class KLogging {
  val logger = KLogger(LoggerFactory.getLogger(javaClass))
}

class KLogger(val logger: Logger) : Logger by logger {

  inline fun trace(msg: () -> Any?) {
    if (isTraceEnabled) trace(msg.toStringSafe())
  }

  /**
   * Lazy add a log message if isDebugEnabled is true
   */
  inline fun debug(msg: () -> Any?) {
    if (isDebugEnabled) debug(msg.toStringSafe())
  }

  /**
   * Lazy add a log message if isInfoEnabled is true
   */
  inline fun info(msg: () -> Any?) {
    if (isInfoEnabled) info(msg.toStringSafe())
  }

  /**
   * Lazy add a log message if isWarnEnabled is true
   */
  inline fun warn(msg: () -> Any?) {
    if (isWarnEnabled) warn(msg.toStringSafe())
  }

  /**
   * Lazy add a log message if isErrorEnabled is true
   */
  inline fun error(msg: () -> Any?) {
    if (isErrorEnabled) error(msg.toStringSafe())
  }

  /**
   * Lazy add a log message with throwable payload if isTraceEnabled is true
   */
  inline fun trace(t: Throwable, msg: () -> Any?) {
    if (isTraceEnabled) trace(msg.toStringSafe(), t)
  }

  /**
   * Lazy add a log message with throwable payload if isDebugEnabled is true
   */
  inline fun debug(t: Throwable, msg: () -> Any?) {
    if (isDebugEnabled) debug(msg.toStringSafe(), t)
  }

  /**
   * Lazy add a log message with throwable payload if isInfoEnabled is true
   */
  inline fun info(t: Throwable, msg: () -> Any?) {
    if (isInfoEnabled) info(msg.toStringSafe(), t)
  }

  /**
   * Lazy add a log message with throwable payload if isWarnEnabled is true
   */
  inline fun warn(t: Throwable, msg: () -> Any?) {
    if (isWarnEnabled) warn(msg.toStringSafe(), t)
  }

  /**
   * Lazy add a log message with throwable payload if isErrorEnabled is true
   */
  inline fun error(t: Throwable, msg: () -> Any?) {
    if (isErrorEnabled) error(msg.toStringSafe(), t)
  }

  @Suppress("NOTHING_TO_INLINE")
  inline fun (() -> Any?).toStringSafe(): String {
    return try {
      invoke().toString()
    } catch (e: Exception) {
      "Log message invocation failed: $e"
    }
  }

}

fun KLogger.debug(host: Host, msg: () -> Any?) {
  if (isDebugEnabled) debug(host.marker, msg.toStringSafe())
}

fun KLogger.info(host: Host, msg: () -> Any?) {
  if (isInfoEnabled) info(host.marker, msg.toStringSafe())
}

fun KLogger.warn(host: Host, msg: () -> Any?) {
  if (isWarnEnabled) warn(host.marker, msg.toStringSafe())
}

fun KLogger.error(host: Host, msg: () -> Any?) {
  if (isErrorEnabled) error(host.marker, msg.toStringSafe())
}
