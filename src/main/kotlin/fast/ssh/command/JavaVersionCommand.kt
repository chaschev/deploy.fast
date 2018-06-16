package fast.ssh.command

import fast.ssh.ConsoleSession
import fast.ssh.IConsoleProcess
import fast.ssh.plainCmd
import fast.ssh.process.Console
import fast.ssh.tryFind

class JavaVersionCommand(override val process: IConsoleProcess) : ConsoleCommand<JavaVersion>(process) {
  init {
    printToOutput = false
  }

  companion object {
    fun javaVersion(session: ConsoleSession): JavaVersionCommand =
      JavaVersionCommand(session.plainCmd("java -version"))
  }


  override fun parseConsole(console: Console): CommandResult<JavaVersion> =
    CommandResult<JavaVersion>(console).withValue({ parseJavaVersion(console.stdout.toString()) })

  internal fun parseJavaVersion(s: String): JavaVersion {
    val isOpenJDK = s.contains("openjdk")

    val g = "version\\s\"1.(\\d+).\\d+_(\\d+)\"".toRegex().tryFind(s)

    val version = if (g != null) {
      listOf(g[1], g[2])
    } else {
      val major = "version \"(\\d+)".toRegex().tryFind(s)!![1]
      val build = "build (\\d+)".toRegex().tryFind(s)!![1]
      listOf(major, build)
    }.map { it.toInt() }

    return JavaVersion(isOpenJDK, SimpleVersion(version as List<Comparable<Any>>))
  }

}