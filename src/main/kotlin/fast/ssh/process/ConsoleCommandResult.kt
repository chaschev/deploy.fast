package fast.ssh.process

data class ConsoleCommandResult(
    val console: Console,
    val exitCode: Int? = null,
    val isEOF: Boolean = true,
    val isTimeout: Boolean = false,
    var timeMs: Long = 0
) {
    fun isOk(): Boolean = exitCode == 0 && !isTimeout

    override fun toString(): String = "ConsoleCommandResult{isOk=${isOk()}, exitCode=$exitCode, timeMs=$timeMs}"
}