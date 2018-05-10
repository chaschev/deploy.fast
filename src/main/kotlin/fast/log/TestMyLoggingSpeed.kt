package fast.log

import java.time.Duration
import java.time.LocalDateTime

/**
 * This is a synthetic test: writing messages into "nowhere" OutputStream
 *
 * Best speed 19m+ messages per second which is 20x faster than log4j. Log4j uses a pattern layout and a high speed server
 * Don't care about the speed anymore, ok
 * The core is fast, anything else depends on the implementation of transformations, i.e.
 *  pattern layout can be made in a DateTimeFormatter fashion, as a sequence of "stamping" operations for Appendable
 *    date? stamp a new date (can be slower)
 *    thread? stamp thread local formatted version of a thread
 *    level? stamp formatted level
 *
 * Results are:
 *  Fastest is BufferedWriter!
 *  Medium speed is Formatter.format + BufferedWriter
 *  Slowest is PrintStream.format
 *  Dead slow is String.format
 *
 *  PrintStream.writeBytes is 2x slower than BufferedWriter.
 *
 *  Finished for now. Want to check? You are welcome into NowhereAppender!
 *
 *  The speed is
 */
object TestMyLoggingSpeed {
  @JvmStatic
  fun main(args: Array<String>) {
    OkLogContext.okLog = OkLogContext {
      any {
        //        classifyBase { it.name == "vm1" }
        withTransformer(PatternTransformer())
        intoAppenders(
          NowhereAppender("nowhere1")
        )
      }
    }.apply {
      debugMode = true
    }

    val logger = OkLogContext.okLog.getLogger("simple")

    logger.info { "info" }

    logger.doPerformanceTest(19000000, "warm up")
    logger.doPerformanceTest(19000000, "real test")
  }

  private fun LoggerImpl<Any, Any>.doPerformanceTest(max: Int, title: String) {
    val started = LocalDateTime.now()

    for (i in 1..max) {
      info { "hey! what'title up" }
    }

    val finished = LocalDateTime.now()

    println("$title: produced $max messages in ${Duration.between(started, finished)}")
  }
}