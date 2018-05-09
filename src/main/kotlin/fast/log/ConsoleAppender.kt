package fast.log

class ConsoleAppender(override val name: String) : Appender<Any, Any> {
  override fun append(obj: Any) {
    print(obj.toString())
  }

  override fun transform(transformer: Transformer<Any, Any>, classifier: Any?, obj: Any, level: LogLevel) {
    transformer.transformIntoText(classifier, obj, System.out, System.out, level)
  }
}