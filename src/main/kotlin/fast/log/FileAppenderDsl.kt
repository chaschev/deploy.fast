package fast.log

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class FileAppenderDsl(
  val file: File,
  override val name: String = file.nameWithoutExtension,
  val autoFlush: Boolean = true
) : Appender<Any, Any> {
  init {
    file.absoluteFile.parentFile.mkdirs()
    file.createNewFile()
  }

  val writer = FileOutputStream(file, true).bufferedWriter()

  override fun supportsTransform() = true

  override fun transform(
    transformer: Transformer<Any, Any>,
    classifier: Any?,
    obj: Any,
    level: LogLevel
  ) {
    transformer.transformIntoText(classifier, obj, writer, writer, level)
  }

  override fun append(obj: Any) {
    writer.write(obj.toString())
  }

  companion object {
    fun fileAppender(
      file: File,
      name: String = file.nameWithoutExtension,
      autoFlush: Boolean = true,
      block: FileAppenderDsl.() -> Unit
    ): FileAppenderDsl {
      return FileAppenderDsl(file, name, autoFlush).apply(block)
    }
  }
}