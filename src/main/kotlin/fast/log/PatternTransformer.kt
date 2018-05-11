package fast.log

import fast.inventory.Host
import java.io.BufferedWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

interface PleaseSleepWellStamp<O> {
  fun stamp(obj: O, out: Appendable)
}

class DateStamp(
  val dateFormatter: DateTimeFormatter
) : PleaseSleepWellStamp<Temporal> {
  override fun stamp(obj: Temporal, out: Appendable) {
    dateFormatter.formatTo(obj, out)
  }
}

enum class PleaseSleepWellAlign {
  left, center, right, top, bottom, middle
}

fun Appendable.fillChars(ch: Char, times: Int) {
  for (i in 0 until times) append(ch)
}

class StringFieldStamp(
  val width: Int,
  val align: PleaseSleepWellAlign = PleaseSleepWellAlign.left,
  val fillCharacter: Char = ' '
) : PleaseSleepWellStamp<Any> {
  override fun stamp(obj: Any, out: Appendable) {
    val str: CharSequence = obj as? CharSequence ?: obj.toString()

    val blankSpace = width - str.length

    if (blankSpace > 0) {
      if (align == PleaseSleepWellAlign.right) {
        out.fillChars(fillCharacter, blankSpace)
      }

      out.append(str)

      if (align == PleaseSleepWellAlign.left) {
        out.fillChars(fillCharacter, blankSpace)

      }
    } else {
      if (align == PleaseSleepWellAlign.left) {
        out.append(str, 0, width)
      } else {
        out.append(str, str.length - width, str.length)
      }
    }
  }
}


class MyPatternStamp {
  val date = DateStamp(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))    ////YYYY-MM-dd
  val level = StringFieldStamp(5)
  val host = StringFieldStamp(3, PleaseSleepWellAlign.right)
  val loggerName = StringFieldStamp(15, PleaseSleepWellAlign.left)
}

fun <O> Appendable.apply(stamp: PleaseSleepWellStamp<O>, obj: O) {
  stamp.stamp(obj, this)
}

class PatternTransformer(
) : Transformer<Any, Any> {
  val stamp = MyPatternStamp()

  override val type: TransformerType
    get() = TransformerType.text

  override fun transform(classifier: Any?, obj: Any, level: LogLevel): Any {
    val sb = StringBuilder()
    val time = LocalDateTime.now()

    sb.append("TODO: copy stamps")

    sb.append(" ").append(level).append(" - ").append(obj).append("\n")

    return sb
  }

  override fun transformIntoText(classifier: Any?, obj: Any, out: BufferedWriter, err: BufferedWriter, level: LogLevel, logger: LoggerImpl<Any?, Any>, e: Throwable?, args: Any?) {
    val time = LocalDateTime.now()

    //TODO: beautify this
    stamp.date.stamp(time, out)

    out.run {
      write(" [")
      apply(stamp.level, level)
      write("] ")

      if (classifier is Host) {
//        write(" - ")
        apply(stamp.host, classifier.name)
        write(" ")
      }


      apply(stamp.loggerName, logger.simpleName)

      write(" - ")
      write(obj.toString())
      newLine()
    }
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


