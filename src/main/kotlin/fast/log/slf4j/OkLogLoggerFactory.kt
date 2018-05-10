package fast.log.slf4j

import fast.log.OkLogContext
import org.slf4j.ILoggerFactory
import org.slf4j.Logger

class OkLogLoggerFactory : ILoggerFactory {
  override fun getLogger(name: String): Logger {
    return OkLogContext.okLog.getLogger(name)
  }

}