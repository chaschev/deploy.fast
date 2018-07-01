package fast.log

import fast.lang.initLater
import fast.log.slf4j.Slf4jLoggerImpl
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

  fun getAppender(name: String): Appender<Any, Any>? {
    if (debugMode) println("getAppender($name)")

    return appenderMap.get(name) as Appender<Any, Any>?
  }


  fun getLogger(name: String): Slf4jLoggerImpl<Any, Any> {
    return getClassifiedLogger(name, null)
  }


  fun <C> getClassifiedLogger(name: String, classifier: C?): Slf4jLoggerImpl<Any, Any> {
    if (debugMode) println("getLogger($name, $classifier)")

    val logger = Slf4jLoggerImpl<Any, Any>(name, classifier)
    val dsl = LoggerFactoryDSL(logger, this)

    logger.level = dsl.defaultLevel

    dsl.apply(_dsl)

    return logger
  }

  companion object {
    var okLog: OkLogContext by initLater(finalize = true)

    val DEFAULT_MUTE = listOf(
      "net.schmizz.sshj.DefaultConfig" to LogLevel.ERROR,
      "net.schmizz" to LogLevel.WARN,
      "io.netty" to LogLevel.WARN,
      "com.datastax" to LogLevel.WARN,
      "com.hazelcast" to LogLevel.INFO
    )

    fun simpleConsoleLogging() =
      simpleConsoleLogging(DEFAULT_MUTE)

    fun simpleConsoleLogging(
       mute: List<Pair<String, LogLevel>>
    )
    {
      val console = ConsoleAppender("console", true)

      OkLogContext.okLog = OkLogContext {
        rules {
          mute {
            applyTo("*")

            for ((pack, level) in mute) {
              pack to level
            }
          }
        }

        //default messaging processing: no classifier specified - dump to console
        any("default") {
          classifyBase { it == null }
          classifyMsg { c: Any? -> c == null }
          withTransformer(PatternTransformer())
          intoAppenders(console)
        }
      }.apply {
        debugMode = false
      }
    }

  }
}