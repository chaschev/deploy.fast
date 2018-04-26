package fast.dsl.ext

import fast.dsl.*
import fast.runtime.AppContext
import fast.runtime.TaskContext
import fast.ssh.command.Regexes
import fast.ssh.runAndWait

class AptTasks(val ext: AptExtension) : NamedExtTasks(ext as DeployFastExtension<ExtensionConfig>) {
  suspend fun listInstalled(filter: String, timeoutMs: Int = 10000): ExtensionTask {
    val task = ExtensionTask("listInstalledPackages", extension = extension) {
      val value = it.ssh.runAndWait(timeoutMs, "apt list --installed | grep $filter",
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

      return@ExtensionTask TaskValueResult<Set<String>?>(value)
    }

    return task
  }

}



class AptExtensionConfig: ExtensionConfig

/**
 * This extension will generate vagrant project file.
 */
class AptExtension(
  app: AppContext,
  config: (TaskContext) -> AptExtensionConfig
) : DeployFastExtension<AptExtensionConfig>("apt", config) {
  override val tasks: (TaskContext) -> AptTasks = {AptTasks(this@AptExtension)}

}


