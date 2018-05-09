package fast.runtime

import fast.ssh.command.ConsoleLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.MarkerManager
import org.apache.logging.log4j.ThreadContext
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.FileAppender
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

//TODO for logging
// TODO text output into separate files
// TODO sessions log with a session marker - make a project requirement if needed
// TODO aggregated log for fast search
// TODO try ctx.info
// TODO try KLogging.info(marker, msg)
object TestLogging  {
  @JvmStatic
  fun main(args: Array<String>) {

//    println(logger)

    val ctx = LogManager.getContext(false) as LoggerContext
    val config = ctx.configuration

    val layout = PatternLayout.newBuilder()
      .withConfiguration(config)
      .withPattern("%d{HH:mm:ss.SSS} %level %msg%n")
      .build()

    val appender = CrawlersFastApp.FixedFileAppender.newFileAppend()
      .setConfiguration(config)
      .withName("ssh.out.appender")
      .withLayout(layout)
//        .withFilter(
//          MarkerFilter.createFilter(
//
//          )
//        )
      .withFileName("logs/java.log")
      .build()

    appender.start()
    config.addAppender(appender)

    ThreadContext.put("threadName", Thread.currentThread().name);


    config.addLogger(ctx, appender)

    val logger1 = LogManager.getLogger("test")
    val warnLevelLogger = LogManager.getLogger("net.schmizz")

//    logger1.info("hi info")
//    logger1.debug("hi debug")

//    warnLevelLogger.debug("that is disabled in log4j.xml")
//    warnLevelLogger.debug("that is disabled in log4j.xml")

    val loggerSshOut = LogManager.getLogger("ssh.out")

//    loggerSshOut.warn(ConsoleLogging.SSH_OUT_MARKER, "ssh out")

    logger1.info(MarkerManager.getMarker("192.168.5.10"), "ip777 out\n")

    val kLogger = LoggerFactory.getLogger("test")
    kLogger.warn(MarkerFactory.getMarker("192.168.5.10"), "ip666 out\n")

//      loggerSshOut.warn(IP2_MARKER, "ip2 out")
//    }

  }

  fun Configuration.addLogger(ctx: LoggerContext, appender: FileAppender) {
    val ref = AppenderRef.createAppenderRef(appender.name, null, null)
    val refs = arrayOf(ref)

    addLogger("programmaticLogger", LoggerConfig.createLogger(false, Level.INFO, "programmaticLogger", "false", refs, null, this, null)
      .apply {
        addAppender(appender, null, null)
      })

    ctx.updateLoggers()
  }

}

