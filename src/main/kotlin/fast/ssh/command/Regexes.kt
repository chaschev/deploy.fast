package fast.ssh.command

object Regexes {
    val NEW_LINE = "(\r\n|\n|\r)".toRegex()
    val ERRORS = "(error|warn|critical|can't|could|fail|not found)".toRegex(
        setOf(
            RegexOption.MULTILINE,
            RegexOption.IGNORE_CASE
        ))

    val SUCCESSES = "(success|done|finished)".toRegex(
        setOf(
            RegexOption.MULTILINE,
            RegexOption.IGNORE_CASE
        ))
}

fun MatchGroup.getLine(original: CharSequence): String {
    if(original.isEmpty()) return original.toString()

    val startIndex = original.lastIndexOf('\n', range.start) + 1
    var endIndex = original.indexOf('\n', range.endInclusive)

    if(endIndex == -1) {
        endIndex = original.length
    }

    //\r\n case
    if(Character.isWhitespace(original[endIndex-1])) endIndex--

    return original.substring(startIndex, endIndex)
}
