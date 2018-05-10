package fast.log.slf4j

import fast.log.LogLevel
import fast.log.LoggerImpl
import org.slf4j.Logger
import org.slf4j.Marker

open class Slf4jLoggerImpl<BC,O>(name: String, classifier: BC? = null):
  LoggerImpl<BC, O>(name, classifier),
  Logger
{
  override fun getName(): String {
    return name
  }

  override fun info(msg: String) {
    log(LogLevel.info, null, {msg as O})
  }

  override fun info(format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun info(format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun info(format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun info(msg: String?, t: Throwable?) {
    TODO("not implemented")
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

  override fun isInfoEnabled(marker: Marker?): Boolean {
    TODO("not implemented")
  }

  override fun warn(msg: String?) {
    TODO("not implemented")
  }

  override fun warn(format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun warn(format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun warn(format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun warn(msg: String?, t: Throwable?) {
    TODO("not implemented")
  }

  override fun warn(marker: Marker?, msg: String?) {
    TODO("not implemented")
  }

  override fun warn(marker: Marker?, format: String?, arg: Any?) {
    TODO("not implemented")
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



  override fun isErrorEnabled(): Boolean {
    TODO("not implemented")
  }

  override fun isErrorEnabled(marker: Marker?): Boolean {
    TODO("not implemented")
  }

  override fun error(msg: String?) {
    TODO("not implemented")
  }

  override fun error(format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun error(format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun error(format: String?, vararg arguments: Any?) {
    TODO("not implemented")
  }

  override fun error(msg: String?, t: Throwable?) {
    TODO("not implemented")
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

  override fun isDebugEnabled(): Boolean {
    TODO("not implemented")
  }

  override fun isDebugEnabled(marker: Marker?): Boolean {
    TODO("not implemented")
  }

  override fun debug(msg: String?) {
    TODO("not implemented")
  }

  override fun debug(format: String?, arg: Any?) {
    TODO("not implemented")
  }

  override fun debug(format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun debug(format: String?, vararg arguments: Any?) {
    TODO("not implemented")
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
    TODO("not implemented")
  }

  override fun trace(format: String?, arg1: Any?, arg2: Any?) {
    TODO("not implemented")
  }

  override fun trace(format: String?, vararg arguments: Any?) {
    TODO("not implemented")
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

  override fun isWarnEnabled(): Boolean {
    TODO("not implemented")
  }

  override fun isWarnEnabled(marker: Marker?): Boolean {
    TODO("not implemented")
  }

  override fun isTraceEnabled(): Boolean {
    TODO("not implemented")
  }

  override fun isTraceEnabled(marker: Marker?): Boolean {
    TODO("not implemented")
  }

}