package fast.log

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.PrintStream
import java.io.Writer
import java.time.format.DateTimeFormatter
import java.util.*

class ConsoleAppender(
  override val name: String,
  override val autoFlush: Boolean = true
) : WriterAppender<Any, Any> {
  private val out = System.out.bufferedWriter()

  override val writer: BufferedWriter
    get() = out


}

class NowhereAppender(override val name: String) : Appender<Any, Any> {
  val nowhere = BufferedWriter(NowhereWriter(), 1024 * 100)
  val nowherePrintStream = PrintStream(NowhereOutputStream())

  val formatter = Formatter(nowhere)

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss.SSS")


  override fun append(obj: Any) {
//    obj.toString()

//    PrintStream()
  }

  override fun supportsTransform(): Boolean  = true

  override fun transform(transformer: Transformer<Any, Any>, classifier: Any?, obj: Any, level: LogLevel, logger: LoggerImpl<Any?, Any>, e: Throwable?, args: Any?) {
    // fastest
    nowhere.write(level.name)
    nowhere.write(classifier.toString())

    //slower
//    formatter.format("%s", level.name.toByteArray())

// slowest
//    nowherePrintStream.format("%s", level.name.toByteArray())
//    nowherePrintStream.write(classifier.toString().toByteArray())

  }
}

class NowhereOutputStream: OutputStream() {
  override fun write(b: Int) {
  }
}

class NowhereWriter: Writer() {
  override fun write(cbuf: CharArray?, off: Int, len: Int) {
  }

  override fun flush() {
  }

  override fun close() {
  }

}