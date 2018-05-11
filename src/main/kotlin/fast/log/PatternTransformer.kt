package fast.log

import java.io.BufferedWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PatternTransformer(
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS") //YYYY-MM-dd
) : Transformer<Any, Any> {
  override val type: TransformerType
    get() = TransformerType.text

  override fun transform(classifier: Any?, obj: Any, level: LogLevel): Any {
    val sb = StringBuilder()
    val time = LocalDateTime.now()

    dateFormatter.formatTo(time, sb)

    sb.append(" ").append(level).append(" - ").append(obj).append("\n")

    return sb
  }

  override fun transformIntoText(classifier: Any?, obj: Any, out: BufferedWriter, err: BufferedWriter, level: LogLevel, logger: LoggerImpl<Any?, Any>, e: Throwable?, args: Any?) {
    val time = LocalDateTime.now()

    //TODO: beautify this
    //TODO: add logger as a parameter
    dateFormatter.formatTo(time, out)
    out.write(" ")
    out.write(level.toString())
    out.write(" [")
    out.write(logger.simpleName)
    out.write("] - ")
    out.write(obj.toString())
    out.newLine()
  }
}

class PlainTextTransformer(
) : Transformer<Any, Any> {
  override val type: TransformerType
    get() = TransformerType.text

  override fun transform(classifier: Any?, obj: Any, level: LogLevel): Any {
    return obj.toString()
  }

  override fun transformIntoText(classifier: Any?, obj: Any, out: BufferedWriter, err: BufferedWriter, level: LogLevel, logger: LoggerImpl<Any?, Any>, e: Throwable?, args: Any?) {
    out.write(obj.toString())
  }
}


