package fast.ssh.process

import fast.ssh.cuteCut
import java.io.BufferedWriter

class Console(
  private val stdin: BufferedWriter,
  internal val session: Cancellable,
  val stdout: StringBuilder = StringBuilder(),
  val stderr: StringBuilder = StringBuilder()
) {
  lateinit var result: ConsoleCommandResult

  fun isRunning(): Boolean  {TODO("check if running via session")}

  fun closeSession() = session.cancel()

  var newOut: String = ""
  var newErr: String = ""

  fun newText(): String = newOut + newErr

  fun writeln(text: String) = write("$text\n")

  fun write(text: String) {
    stdin.write(text)
    stdin.flush()
  }

  override fun toString(): String {
    return "Console(result=$result, stdout=${stdout.cuteCut(40)})"
  }
}