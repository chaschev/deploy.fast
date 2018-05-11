package fast.log

class RestrictionsRulesDsl(override val applyTo: ArrayList<String>) : AppliedRules {
  constructor(vararg applyTo: String) :
    this(applyTo.toCollection(ArrayList()) as ArrayList<String>)

  var level: LogLevel = LogLevel.INFO

  val regexRules = ArrayList<Pair<Regex, LogLevel>>()
  val prefixRules = ArrayList<Pair<String, LogLevel>>()

  fun applyTo(ref: String) {applyTo.add(ref)}

  infix fun String.to(level: LogLevel) { prefixRules += Pair(this, level) }
  infix fun Regex.to(level: LogLevel) { regexRules += Pair(this, level) }

  override fun apply(logger: LoggerImpl<*, *>) {
    for ((prefix, level) in prefixRules) {
      if(logger.name.startsWith(prefix)) {
        logger.setIfHigher(level)
        return
      }
    }

    for ((regex, level) in regexRules) {
      if(logger.name.matches(regex)) {
        logger.setIfHigher(level)
        return
      }
    }
  }

}