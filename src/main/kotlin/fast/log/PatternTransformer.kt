package fast.log

import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

class FastPatternTransformer(
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