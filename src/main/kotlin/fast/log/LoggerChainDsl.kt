package fast.log

class LoggerChainDsl<BC, O>(
  val logger: LoggerImpl<BC, O>,
  val name: String? = null
) {

  val chain = LoggerImpl.MessageChain(logger)

  fun setLevel(level: LogLevel) {
    logger.level = level
  }

  private var matched = true

  fun <C, O> withFilter(messageFilter: MessageFilter<C, O>) {
    chain.filters.add(messageFilter)
  }

  fun filterBase(filter: (LoggerImpl<BC, O>) -> Boolean) {
    matched = matched && filter(logger)

    if (OkLogContext.okLog.debugMode && !matched) println(" filterBase matched=$matched for ${logger.name}")

    if (!matched) throw ConfBranchDidntMatchException()
  }


  fun classifyBase(filter: (BC?) -> Boolean) {
    matched = matched && filter(logger.classifier)

    if (OkLogContext.okLog.debugMode && !matched) println(" classifyBase matched=$matched for ${logger.name}")


    if (!matched) throw ConfBranchDidntMatchException()
  }

  fun <C> filter(filter: (O) -> Boolean) {
    chain.filters.add(MessageFilter.ofObj(filter))
  }

  fun classifyMsgObj(filter: (classifier: Any?, msg: O) -> Boolean) {
    chain.filters.add(MessageFilter.ofObj(filter))
  }

  fun classifyMsg(filter: (classifier: Any?) -> Boolean) {
    chain.filters.add(MessageFilter.of({c: Any?, _ ->  filter(c)}))
  }

  fun <C> filterMessages(filter: (C?, O, LogLevel) -> Boolean) {
    chain.filters.add(MessageFilter.ofObj(filter))
  }

  fun filterLevel(levelAtLeast: LogLevel) {
    chain.filters.add(MessageFilter.of {it >= levelAtLeast} )
  }

  fun <C> filterMessages(filter: MessageFilter<C, O>) {
    chain.filters.add(filter)
  }

  fun <C> withTransformer(transformer: Transformer<C, O>) {
    chain.transformer = transformer as Transformer<Any, O>
  }

  fun <C> intoAppenders(vararg appenders: Appender<C, O>) {
    chain.appenders.addAll(appenders as Array<out Appender<Any, Any>>)
    if(!logger.chains.contains(chain)) logger.chains += chain
  }


}
