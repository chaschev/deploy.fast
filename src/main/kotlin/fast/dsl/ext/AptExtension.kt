package fast.dsl.ext

import fast.dsl.*
import fast.runtime.TaskContext
import fast.ssh.command.Regexes
import fast.ssh.process.Console
import fast.ssh.runAndWait

class AptTasks(val ext: AptExtension, taskCtx: TaskContext) : NamedExtTasks(
  ext as DeployFastExtension<ExtensionConfig>, taskCtx
) {
  suspend fun listInstalled(filter: String, timeoutMs: Int = 10000): Set<String> {
    val task = ExtensionTask("listInstalledPackages", extension) {

      val value = ssh.runAndWait(timeoutMs,
        cmd = "apt list --installed | grep $filter",
        process = {console->
          val items = cutAfterCLIInterface(console)

          val result = items.map { it.substringBefore("/") }.toHashSet()

          result
        },
        processErrors = {emptySet<String>()}
      ).value!!

      TaskValueResult(value)
    }

    return (taskCtx.play(task) as TaskValueResult<Set<String>?>).value!!
  }



  suspend fun install(
    pack: String,
    options: InstallOptions = InstallOptions.DEFAULT,
    timeoutMs: Int = 600 * 1000
  ): CommandResult<String> =
    provider.runAndWaitInteractive(timeoutMs, "apt-get install -y ${options.asString()} $pack",
      ConsoleProcessing(
        process = { "ok" },
        consoleHandler = {
          val newText = it.newText()
          print(newText)

          if (newText.contains("What would you like to do about it ?")) {
            it.writeln("Y")
          }
        }
      )
    )

  suspend fun update(options: InstallOptions = InstallOptions.DEFAULT,
                     timeoutMs: Int = 600 * 1000): CommandResult<String> =
    provider.runAndWait(timeoutMs, "apt-get update -y ${options.asString()}",
      { "ok" })


  suspend fun remove(pack: String, timeoutMs: Int = 600 * 1000): String? =
    provider.runAndWait(timeoutMs, "apt-get remove -y $pack", { "ok" }).value

  suspend fun dpkgRemove(pack: String, timeoutMs: Int = 600 * 1000): String? =
    provider.runAndWait(timeoutMs, "dpkg --remove $pack", { "ok" }).value


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

  suspend fun dPkgList(filter: String, timeoutMs: Int = 10000): List<DPkgPackageInfo>? =
    provider.runAndWait(timeoutMs, "dpkg --list *$filter*",
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
      }).value

  suspend fun dpkgListInstalled(filter: String, timeoutMs: Int = 10000): List<DPkgPackageInfo>? =
    dPkgList(filter, timeoutMs)?.filter { it.installed }



  private fun cutAfterCLIInterface(console: Console): List<String> {
    val items = if (console.stdout.contains("CLI interface")) {
      val list = console.stdout.trim().toString()
        .substringAfter("CLI interface")
        .split(Regexes.NEW_LINE)

      list.subList(2, list.size)
    } else {
      console.stdout.toString().split(Regexes.NEW_LINE)
    }
    return items
  }

}



class AptExtensionConfig: ExtensionConfig

/**
 * This extension will generate vagrant project file.
 */
class AptExtension(
  config: (TaskContext) -> AptExtensionConfig
) : DeployFastExtension<AptExtensionConfig>("apt", config) {
  override val tasks: (TaskContext) -> AptTasks = {AptTasks(this@AptExtension, it)}

}


