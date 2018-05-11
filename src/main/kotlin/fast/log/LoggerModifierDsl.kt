package fast.log

class LoggerModifierDsl<BC, O>(
  val logger: LoggerImpl<BC, O>
) {

  fun setLevel(level: LogLevel) {
    logger.level = level
  }

  private var matched = true

  fun <C, O> withFilter(messageFilter: MessageFilter<C, O>) {
    logger.filters.add(messageFilter)
  }

  fun filterBase(filter: (LoggerImpl<BC, O>) -> Boolean) {
    matched = matched && filter(logger)

    if (OkLogContext.okLog.debugMode) println(" filterBase matched=$matched")

    if (!matched) throw ConfBranchDidntMatchException()
  }


  fun classifyBase(filter: (BC?) -> Boolean) {
    val c = logger.classifier

    if (c != null) {
      matched = matched && filter(c)
    } else {
      matched = false
    }

    if (OkLogContext.okLog.debugMode) println(" classifyBase matched=$matched")


    if (!matched) throw ConfBranchDidntMatchException()
  }

  fun <C> filter(filter: (O) -> Boolean) {
    logger.filters.add(MessageFilter.ofObj(filter))
  }

  fun <C> classifyMsg(filter: (classifier: C?, msg: O) -> Boolean) {
    logger.filters.add(MessageFilter.ofObj(filter))
  }

  fun <C> classifyMsg(filter: (classifier: C?) -> Boolean) {
    logger.filters.add(MessageFilter.of({c: C?, _ ->  filter(c)}))
  }

  fun <C> filterMessages(filter: (C?, O, LogLevel) -> Boolean) {
    logger.filters.add(MessageFilter.ofObj(filter))
  }

  fun filterLevel(levelAtLeast: LogLevel) {
    logger.filters.add(MessageFilter.of {it >= levelAtLeast} )
  }

  fun <C> filterMessages(filter: MessageFilter<C, O>) {
    logger.filters.add(filter)
  }

  fun <C> intoAppenders(vararg appenders: Appender<C, O>) {
    logger.appenders.addAll(appenders as Array<out Appender<Any, Any>>)
  }

  fun <C> withTransformer(transformer: Transformer<C, O>) {
    logger.transformer = transformer as Transformer<Any, O>
  }
}