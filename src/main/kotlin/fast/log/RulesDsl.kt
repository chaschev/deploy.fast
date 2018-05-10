package fast.log

class RulesDsl {
  val list = ArrayList<AppliedRules>()

  fun restrict(block: RestrictionsRulesDsl.() -> Unit) {
    list += RestrictionsRulesDsl().apply(block)
  }
}