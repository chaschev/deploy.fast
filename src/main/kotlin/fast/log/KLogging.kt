package fast.log

import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class KLogging {
  val logger = KLogger(LoggerFactory.getLogger(javaClass))
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
  all, trace, debug, info, warn, error, fatal, none
}

enum class TransformerType {
  obj, text, binary
}

interface Transformer<C, O> {
  val type: TransformerType

  fun transform(classifier: C?, obj: O, level: LogLevel): Any = TODO()
  fun transformIntoText(classifier: C?, obj: O, out: PrintStream, err: PrintStream, level: LogLevel): Unit =
    TODO("that's GC-free version which probably is not supported yet")
  fun transformIntoBinary(classifier: C?, obj: O, out: BufferedWriter, err: BufferedWriter): Unit =
    TODO("that's GC-free version which probably is not supported yet")
}

class PatternTransformer(
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss.SSS")
) : Transformer<Any, Any> {
  override val type: TransformerType
    get() = TransformerType.text

  override fun transform(classifier: Any?, obj: Any, level: LogLevel): Any {
    val sb = StringBuilder()
    val time = LocalDateTime.now()

    dateFormatter.formatTo(time, sb)

    sb.append(" [").append(level).append("] - ").append(obj).append("\n")

    return sb
  }

  override fun transformIntoText(classifier: Any?, obj: Any, out: PrintStream, err: PrintStream, level: LogLevel) {
    val time = LocalDateTime.now()

    dateFormatter.formatTo(time, out)
    out.format(" [%s] - %s\n", level, obj)
  }
}

class ConfBranchDidntMatchException(msg: String = "conf branch didn't match") : Exception(msg)

fun CharSequence.startsWithAny(prefixes: Iterable<String>): Boolean {
  return prefixes.find { this.startsWith(it) } != null
}

fun CharSequence.startsWithAny(prefixes: Array<out String>): Boolean {
  return prefixes.find { this.startsWith(it) } != null
}


class ConsoleAppender(override val name: String) : Appender<Any, Any> {
  override fun append(obj: Any) {
    print(obj.toString())
  }

  override fun transform(transformer: Transformer<Any, Any>, classifier: Any?, obj: Any, level: LogLevel) {
    transformer.transformIntoText(classifier, obj, System.out, System.out, level)
  }
}

class DebugConsoleAppender(override val name: String) : Appender<Any, Any> {
  override fun append(obj: Any) {
    println("$name - $obj")
  }
}

