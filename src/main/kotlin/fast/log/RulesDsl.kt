package fast.log

class RulesDsl {
  val list = ArrayList<AppliedRules>()

  fun mute(block: RestrictionsRulesDsl.() -> Unit) {
    list += RestrictionsRulesDsl().apply(block)
  }
}