package fast.log

interface AppliedRules {
//  val applyToAll: Boolean
  /* Accepts * */
  val applyTo: List<String>
  fun apply(logger: LoggerImpl<*, *>)
  fun isApplicable(chainName: String?): Boolean =
    (applyTo.contains("*") || applyTo.contains(chainName) )
}