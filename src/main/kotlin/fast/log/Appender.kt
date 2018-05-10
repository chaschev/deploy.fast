package fast.log

interface Appender<C, O> {
  val name: String

  fun append(obj: O)
  fun append(obj: O, level: LogLevel) = append(obj)
  fun append(obj: O, classifier: C?, level: LogLevel) = append(obj)

  fun transform(transformer: Transformer<C, O>, classifier: C?, obj: O, level: LogLevel): Unit =
    TODO("transform() is not yet there")

  fun supportsTransform() = false
}

