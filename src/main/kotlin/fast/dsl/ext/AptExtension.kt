package fast.dsl.ext

import fast.dsl.*
import fast.runtime.AppContext
import fast.runtime.TaskContext
import fast.ssh.command.Regexes
import fast.ssh.runAndWait

class AptTasks : NamedExtTasks() {
  suspend fun listInstalled(filter: String, timeoutMs: Int = 10000): Set<String>? {
    return context.ssh.runAndWait(timeoutMs, "apt list --installed | grep $filter",
      process = {
        val items = if (it.stdout.contains("CLI interface")) {
          val list = it.stdout.trim().toString()
            .substringAfter("CLI interface")
            .split(Regexes.NEW_LINE)

          list.subList(2, list.size)
        } else {
          it.stdout.toString().split(Regexes.NEW_LINE)
        }

        val result = items.map { it.substringBefore("/") }.toHashSet()

        result
      },
      processErrors = {
        emptySet<String>()
      }).value
  }

}



class AptExtensionConfig: ExtensionConfig

/**
 * This extension will generate vagrant project file.
 */
class AptExtension(
  app: AppContext,
  config: (TaskContext) -> AptExtensionConfig
) : DeployFastExtension<AptExtensionConfig>(app, config) {
  override fun getStatus(): ServiceStatus {
    return ServiceStatus(InstalledStatus.installed, RunningStatus.notApplicable)
  }

  override val tasks = AptTasks()
}


