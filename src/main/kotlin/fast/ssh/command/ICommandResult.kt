package fast.ssh.command

import fast.ssh.process.Console

/**
 * Be able to
 *  LS: sync&async. Run `ls` and get a fast readable response: file names, sizes, dirs. Know if newCommand failed
 *  gradle build. Customize waiting time
 *
 *  Therefor:
 *   Explicit timeout for each newCommand. Timeout is a failure
 *   Explicit ok/not ok
 *
 *   Customized newCommand result.
 */

interface ICommandResult<T>{
//    val exitCode: Int?
//    val isTimeout: Boolean
//    val timeMs: Int?
    val console: Console
    val value: T?

    /**
     * Found errors when parsing. If exited with error and didn't parse, should be false
     */
    val hasOutputErrors: Boolean

    fun isOk(): Boolean = !hasOutputErrors && (console.result?.isOk() ?: false)

    fun parsedErrorsConcise(): List<String> = TODO()
    fun parsedErrorsFull(): List<String>  = TODO()
    fun cuteOutput(): String = TODO()

//    fun asTextObject(filter: TextFilter): TextObject = TODO()
}