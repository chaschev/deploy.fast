package fast.log

import fast.log.slf4j.Slf4jLoggerImpl
import org.slf4j.LoggerFactory
import java.io.BufferedWriter

open class KLogging {
  val logger = KLogger(LoggerFactory.getLogger(javaClass))
}

open class OkLogging(classifier: Any? = null) {
  val logger = LoggerFactory.getLogger(javaClass) as Slf4jLoggerImpl<String, String>

  init {
    if(classifier != null) {

    }
  }
}

/*
 LoggerRules:
  a list of filters
  a list of appenders

 Logger:
  filters
  transformers
  appenders


 RootLoggerRules:
  matching(net.schmizz)
    addLevelFilter(warn)

  matching(*)
 */

enum class LogLevel {
  ALL, TRACE, DEBUG, INFO, WARN, ERROR, NONE
}

enum class TransformerType {
  obj, text, binary
}

interface Transformer<C, O> {
  val type: TransformerType

  fun transform(classifier: C?, obj: O, level: LogLevel): Any = TODO()
  fun transformIntoText(classifier: C?, obj: O, out: BufferedWriter, err: BufferedWriter, level: LogLevel, logger: LoggerImpl<Any?, O>, e: Throwable?, args: Any?): Unit =
    TODO("that's GC-free version which probably is not supported yet")
  fun transformIntoBinary(classifier: C?, obj: O, out: BufferedWriter, err: BufferedWriter): Unit =
    TODO("that's GC-free version which probably is not supported yet")
}

class ConfBranchDidntMatchException(msg: String = "conf branch didn't match") : Exception(msg)



