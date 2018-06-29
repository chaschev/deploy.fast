package fast.log.slf4j

import fast.log.LogLevel.*
import fast.log.LoggerImpl
import org.slf4j.Logger
import org.slf4j.Marker


fun Logger.ok(): LoggerImpl<Any, Any> {
  return this as LoggerImpl<Any, Any>
}

open class Slf4jLoggerImpl<BC,O>(name: String, classifier: BC? = null):
  LoggerImpl<BC, O>(name, classifier),
  Logger
{
  override fun getName(): String {
    return name
  }

  override fun info(msg: String) {
    log(INFO, msg as O)
  }

  override fun info(format: String, arg: Any) {
    log(INFO, format as O, args = arg)
  }

  override fun info(format: String?, arg1: Any?, arg2: Any?) {
    log(INFO, format as O, args = *arrayOf(arg1, arg2))
  }

  override fun info(format: String?, vararg arguments: Any?) {
    log(INFO, format as O, args = *arguments)
  }

  override fun info(msg: String?, t: Throwable?) {
    log(INFO, msg as O, e = t)
  }

  override fun info(marker: Marker?, msg: String?) {
    TODO("not implemented")
  }

  override fun info(marker: Marker?, format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun info(marker: Marker?, msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun warn(msg: String) {
    log(WARN, msg as O)
  }

  override fun warn(format: String?, arg: Any?) {
    log(WARN, format as O, args = arg)
  }

  override fun warn(format: String?, vararg arguments: Any?) {
    log(WARN, format as O, args = arguments)
  }

  override fun warn(format: String?, arg1: Any?, arg2: Any?) {
    log(WARN, format as O, args = *arrayOf(arg1, arg2))
  }

  override fun warn(msg: String?, t: Throwable?) {
    log(WARN, msg as O, e = t)
  }

  override fun warn(marker: Marker?, msg: String?) {
    log(WARN, msgClassifier = marker?.name as BC, _obj = msg as O)
  }

  override fun warn(marker: Marker?, format: String?, arg: Any?) {
    log(WARN, msgClassifier = marker?.name as BC, _obj = format as O, args = arg)
  }

  override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun error(msg: String?) {
    log(ERROR, msg as O)
  }

  override fun error(format: String?, arg: Any?) {
    log(ERROR, format as O, args = arg)
  }

  override fun error(format: String?, arg1: Any?, arg2: Any?) {
    log(ERROR, format as O, args = *arrayOf(arg1, arg2))
  }

  override fun error(format: String?, vararg arguments: Any?) {
    log(ERROR, format as O, args = *arguments)
  }

  override fun error(msg: String?, t: Throwable?) {
    log(WARN, msg as O, e = t)
  }

  override fun error(marker: Marker?, msg: String?) {
    TODO("not implemented")
  }

  override fun error(marker: Marker?, format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun error(marker: Marker?, msg: String?, t: Throwable?) {
    TODO("not implemented")
  }



  override fun debug(msg: String) {
    log(DEBUG, msg as O)
  }

  override fun debug(format: String, arg: Any?) {
    log(DEBUG, format as O, args = arg)
  }

  override fun debug(format: String?, arg1: Any?, arg2: Any?) {
    log(DEBUG, format as O, args = *arrayOf(arg1, arg2))
  }

  override fun debug(format: String?, vararg arguments: Any?) {
    log(DEBUG, format as O, args = *arguments)
  }

  override fun debug(msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun debug(marker: Marker?, msg: String?) {
    TODO("not implemented")
  }

  override fun debug(marker: Marker?, format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun trace(msg: String?) {
    TODO("not implemented")
  }

  override fun trace(format: String?, arg: Any?) {
    log(TRACE, format as O, args = arg)
  }

  override fun trace(format: String?, arg1: Any?, arg2: Any?) {
    log(TRACE, format as O, args = *arrayOf(arg1, arg2))
  }

  override fun trace(format: String?, vararg arguments: Any?) {
    log(TRACE, format as O, args = *arguments)
  }

  override fun trace(msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun trace(marker: Marker?, msg: String?) {
    TODO("not implemented")
  }

  override fun trace(marker: Marker?, format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
    TODO("not implemented")
  }

  override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
    TODO("not implemented")
  }



  override fun isTraceEnabled(marker: Marker?): Boolean = isEnabled(TRACE)

  override fun isDebugEnabled(marker: Marker?)= isEnabled(DEBUG)

  override fun isInfoEnabled(marker: Marker?)= isEnabled(INFO)

  override fun isWarnEnabled(marker: Marker?): Boolean = isEnabled(WARN)

  override fun isErrorEnabled(marker: Marker?)= isEnabled(ERROR)

}