package fast.log

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream

class FileAppenderDsl(
  val file: File,
  override val name: String = file.nameWithoutExtension,
  override val autoFlush: Boolean = true
) : WriterAppender<Any, Any> {
  init {
    file.absoluteFile.parentFile.mkdirs()
    file.createNewFile()
  }

  override val writer: BufferedWriter = FileOutputStream(file, true).bufferedWriter()

  override fun supportsTransform() = true

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