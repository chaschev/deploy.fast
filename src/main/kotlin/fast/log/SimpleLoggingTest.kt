package fast.log

import fast.inventory.Host
import fast.log.LogLevel.*
import fast.log.RoutingAppender.Companion.routing
import java.io.File
import fast.log.OkLogContext.Companion.okLog
import fast.log.OkLogContext.Companion.simpleConsoleLogging
import honey.lang.readResource
import org.slf4j.LoggerFactory

object SimpleLoggingTest  {
  @JvmStatic
  fun main(args: Array<String>) {
    simpleConsoleLogging()

    val logger = KLogger(LoggerFactory.getLogger(javaClass))

    logger.warn("{}{}", "a", "b")
  }


}

