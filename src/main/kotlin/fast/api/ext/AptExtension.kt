package fast.api.ext

import fast.api.*
import fast.dsl.*
import fast.ssh.ConsoleProcessing
import fast.ssh.command.Regexes
import fast.ssh.process.Console
import fast.ssh.runAndWait
import fast.ssh.runAndWaitProcess
import fast.ssh.runAndWaitInteractive

class AptConfig : ExtensionConfig

typealias AptTask<R> = ExtensionTask<R, AptExtension, AptConfig>
typealias AptTaskContext = ChildTaskContext<AptExtension, AptConfig>

class AptExtension(
  config: (AptTaskContext) -> AptConfig = { AptConfig() }
) : DeployFastExtension<AptExtension, AptConfig>(
  "apt", config
) {
  override val tasks = { ctx: ChildTaskContext<*, *> ->
    AptTasks(this@AptExtension, ctx)
  }
}


class AptTasks(ext: AptExtension, parentCtx: ChildTaskContext<*, *>) :
  NamedExtTasks<AptExtension, AptConfig>(
    ext, parentCtx
  ) {
  suspend fun listInstalled(filter: String, timeoutMs: Int = 10000): Set<String> {
    val task = AptTask("listInstalledPackages", extension) {
      ssh.runAndWaitProcess(cmd = "sudo apt list --installed | grep $filter",
        process = { console ->
          val items = cutAfterCLIInterface(console)

          val result = items.map { it.substringBefore("/") }.toHashSet()

          result
        },
        processErrors = { emptySet<String>() },
        timeoutMs = timeoutMs
      ).toFast()
    }

    return task.play(extCtx).value!!
  }

  suspend fun requirePackage(
    command: String,
    packageToInstall: String = command,
    timeoutMs: Int = 600 * 1000) =
    ExtensionTask("requirePackage", extension) {
      ssh.runAndWait(
        cmd = "which $command || sudo apt-get install -y $packageToInstall",
        timeoutMs = timeoutMs
      ).toFast(true)

    }.play(extCtx)

  suspend fun install(
    pack: String,
    options: InstallOptions = InstallOptions.DEFAULT,
    timeoutMs: Int = 600 * 1000
  ): ITaskResult<Boolean> {
    return (AptTask("install", extension) {
      //flag for non-interactive mode https://stackoverflow.com/questions/33370297/apt-get-update-non-interactive/33370375#33370375
      ssh.runAndWaitInteractive("sudo apt-get install  -o \"Dpkg::Options::=--force-confold\" -y ${options.asString()} $pack", ConsoleProcessing(
        process = { true },
        consoleHandler = {
          val newText = it.newText()

          if (newText.contains("What would you like to do about it ?")) {
            it.writeln("Y")
          }
        }
      ),
        timeoutMs
      ).toFast(true)
    }.play(extCtx))
  }

  suspend fun update(
    options: InstallOptions = InstallOptions.DEFAULT,
    timeoutMs: Int = 600 * 1000) =
    ExtensionTask("update", extension) {

      ssh.runAndWaitProcess(
        cmd = "sudo apt-get update -y ${options.asString()}",
        process = { "ok" },
        timeoutMs = timeoutMs
      )
        .toFast(true)

    }.play(extCtx)


  suspend fun remove(pack: String, timeoutMs: Int = 600 * 1000) =
    ExtensionTask("update", extension) {
      ssh.runAndWaitProcess(
        "sudo apt-get remove -y $pack",
        { "ok" },
        timeoutMs = timeoutMs
      ).toFast(true)

    }.play(extCtx)


  suspend fun dpkgRemove(pack: String, timeoutMs: Int = 600 * 1000) =
    ExtensionTask("update", extension) {
      ssh.runAndWaitProcess(
        "sudo dpkg --remove $pack",
        { "ok" },
        timeoutMs = timeoutMs
      ).toFast(true)

    }.play(extCtx)


  data class InstallOptions(
    val dpkgForceOverwrite: Boolean? = null,
    val allowUnauthenticated: Boolean? = null
  ) {
    companion object {
      val DEFAULT = InstallOptions()
    }

    fun asString(): String =
      if (dpkgForceOverwrite == true) "-o Dpkg::Options::=\"--force-overwrite\" " else "" +
        if (allowUnauthenticated == true) "--allow-unauthenticated " else ""
  }

  data class DPkgPackageInfo(
    val installed: Boolean,
    val name: String,
    val arch: String,
    val desc: String
  )

  suspend fun dPkgList(filter: String, timeoutMs: Int = 10000) = extensionFun("dPkgList") {
    ssh.runAndWaitProcess(
      cmd = "dpkg --list *$filter*",
      timeoutMs = timeoutMs,
      process = { console ->
        val rows =
          console.stdout.toString()
            .substringAfter("==\n").trim()
            .split("\n")
            .filter { it.isNotEmpty() }
            .map {
              val m = "^(\\w\\w)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+(.*)?$".toRegex().matchEntire(it)!!
              val g = m.groups.map { it?.value ?: "null" }

              DPkgPackageInfo(g[1][0] == 'i', g[2], g[3], g[4])
            }

        rows
      },
      processErrors = { emptyList() }
    ).toFast(true).mapValue { it }
  }

  suspend fun dpkgListInstalled(filter: String, timeoutMs: Int = 10000) =
    (dPkgList(filter, timeoutMs)).mapValue { it?.filter { it.installed } }


  private fun cutAfterCLIInterface(console: Console): List<String> {
    val items = if (console.stdout.contains("CLI interface")) {
      val list = console.stdout.trim().toString()
        .substringAfter("CLI interface")
        .split(Regexes.NEW_LINE)

      when (list.size) {
        0, 1 -> list   //error in parsing output
        else -> list.subList(2, list.size)
      }
    } else {
      console.stdout.toString().split(Regexes.NEW_LINE)
    }
    return items
  }

}



