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

  var defaultLevel = LogLevel.DEBUG
  var rules: RulesDsl? = null

  fun rules(block: RulesDsl.() -> Unit) {
    this.rules = RulesDsl().apply(block)
  }

  fun <C, O> matching(predicate: (String) -> Boolean, block: LoggerChainDsl<C, O>.() -> Unit) {
    LoggerChainDsl(logger as LoggerImpl<C, O>).apply(block)
  }

  fun <C, O> starts(vararg with: String, block: LoggerChainDsl<C, O>.() -> Unit) =
    matching({ it.startsWithAny(with) }, block)

  fun <C, O> matchingClass(predicate: (String, C) -> Boolean, block: LoggerChainDsl<C, O>.() -> Unit) {
    LoggerChainDsl(logger as LoggerImpl<C, O>).apply(block)
  }

  fun all(block: LoggerChainDsl<Any, Any>.() -> Unit): LoggerChainDsl<Any, Any>? {
    val r = nullForException(ConfBranchDidntMatchException::class.java) {
      LoggerChainDsl(logger as LoggerImpl<Any, Any>).apply(block)
    }

    if (ctx.debugMode) println(" dsl.all() = $r")

    return r
  }

  fun <C, O> allCustom(block: LoggerChainDsl<C, O>.() -> Unit) =
    LoggerChainDsl(logger as LoggerImpl<C, O>).apply(block)

  fun any(name: String? = null, block: LoggerChainDsl<Any, Any>.() -> Unit): LoggerChainDsl<Any, Any>? {
    applyRules(name)

    val r = nullForException(ConfBranchDidntMatchException::class.java) {
      LoggerChainDsl(logger as LoggerImpl<Any, Any>).apply(block)
    }

    if (ctx.debugMode) println(" dsl.any() = $r")

    return r
  }

  fun ref(name: String): Appender<Any, Any> {
    return ctx.getAppender(name)!!
  }


  private fun applyRules(chainName: String?) {
    for (rules in rules?.list.orEmpty()) {
      if(rules.isApplicable(chainName)) {
        rules.apply(logger)
      }
    }
  }

  fun <C, O> allWithClassifier(classifier: C, block: LoggerChainDsl<C, O>.() -> Unit) =
    LoggerChainDsl(logger as LoggerImpl<C, O>).apply(block)

  fun appenders(list: List<Appender<*, *>>) {
    for (appender in list) {
      ctx.getAppender(appender.name, { appender })
    }
  }


  companion object {
  }
}