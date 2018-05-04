package fast.ssh.command

import fast.lang.nullForException
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

interface ICommandResult<T> {
  //    val exitCode: Int?
//    val isTimeout: Boolean
//    val timeMs: Int?
  val console: Console

  /* var because can be null */
  var value: T

  /**
   * Found errors when parsing. If exited with error and didn't parse, should be false
   */
  fun hasErrors(): Boolean = !errors().isEmpty() || exception != null || nullForException { !console.result.isOk()} ?: true

  var exception: Exception?

  fun isOk(): Boolean = !hasErrors() && console.result.isOk()

  fun parsedErrorsConcise(): List<String> = TODO()
  fun parsedErrorsFull(): List<String> = TODO()
  fun cuteOutput(): String = TODO()
  fun errors(): MutableList<String>

//    fun asTextObject(filter: TextFilter): TextObject = TODO()
}