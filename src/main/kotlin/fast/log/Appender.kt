package fast.log

import java.io.BufferedWriter

interface Appender<C, O> {
  val name: String

  fun append(obj: O)
  fun append(obj: O, level: LogLevel) = append(obj)
  fun append(obj: O, classifier: C?, level: LogLevel) = append(obj)

  fun transform(transformer: Transformer<C, O>, classifier: C?, obj: O, level: LogLevel, logger: LoggerImpl<Any?, O>, e: Throwable?, args: Any?): Unit =
    TODO("transform() is not yet there")

  fun supportsTransform() = false
}

interface WriterAppender<C,O> : Appender<C,O> {

  val writer: BufferedWriter

  val autoFlush: Boolean

  override fun supportsTransform() = true

  override fun append(obj: O) {
    writer.write(obj.toString())
  }

  override fun transform(
    transformer: Transformer<C, O>,
    classifier: C?,
    obj: O,
    level: LogLevel,
    logger: LoggerImpl<Any?, O>,
    e: Throwable?,
    args: Any?
  ) {
    transformer.transformIntoText(classifier, obj, writer, writer, level, logger, e, args)
    if(autoFlush) writer.flush()
  }

}
