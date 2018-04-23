package fast.ssh.process

import fast.ssh.cuteCut
import java.io.BufferedWriter

class Console(
    val stdin: BufferedWriter,
    internal val session: Cancellable,
    val stdout: StringBuilder = StringBuilder(),
    val stderr: StringBuilder = StringBuilder(),

    var result: ConsoleCommandResult? = null
) {
    fun closeSession()= session.cancel()

    var newIn: String = ""
    var newErr: String = ""


    fun newText(): String = newIn + newErr

    fun writeln(text:String) = write("$text\n")

    fun write(text:String) {
        stdin.write(text)
        stdin.flush()
    }

    override fun toString(): String {
        return "Console(result=$result, stdout=${stdout.cuteCut(40)})"
    }
}