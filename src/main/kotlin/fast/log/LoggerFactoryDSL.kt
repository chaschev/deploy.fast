package fast.log

import fast.lang.nullForException
import honey.lang.startsWithAny

/**
 * These are logger building rules.
 * Each logger is identified by a pair: C, String
 */
class LoggerFactoryDSL(
  val logger: LoggerImpl<*, *>,
  val ctx: OkLogContext
) {

  var defaultLevel = LogLevel.debug

  fun <C, O> matching(predicate: (String) -> Boolean, block: LoggerModifierDsl<C, O>.() -> Unit) {
    LoggerModifierDsl(logger as LoggerImpl<C, O>).apply(block)
  }

  fun <C, O> starts(vararg with: String, block: LoggerModifierDsl<C, O>.() -> Unit) =
    matching({ it.startsWithAny(with) }, block)

  fun <C, O> matchingClass(predicate: (String, C) -> Boolean, block: LoggerModifierDsl<C, O>.() -> Unit) {
    LoggerModifierDsl(logger as LoggerImpl<C, O>).apply(block)
  }

  fun <C> all(block: LoggerModifierDsl<C, Any>.() -> Unit): LoggerModifierDsl<C, Any>? {
    val r = nullForException(ConfBranchDidntMatchException::class.java) {
      LoggerModifierDsl(logger as LoggerImpl<C, Any>).apply(block)
    }

    if (ctx.debugMode) println(" dsl.all() = $r")

    return r
  }

  fun <C, O> allCustom(block: LoggerModifierDsl<C, O>.() -> Unit) =
    LoggerModifierDsl(logger as LoggerImpl<C, O>).apply(block)

  fun any(block: LoggerModifierDsl<*, *>.() -> Unit) =
    LoggerModifierDsl(logger as LoggerImpl<Any, Any>).apply(block)

  fun <C, O> allWithClassifier(classifier: C, block: LoggerModifierDsl<C, O>.() -> Unit) =
    LoggerModifierDsl(logger as LoggerImpl<C, O>).apply(block)

  fun appenders(list: List<Appender<*, *>>) {
    for (appender in list) {
      ctx.getAppender(appender.name, { appender })
    }
  }


  companion object {
  }
}