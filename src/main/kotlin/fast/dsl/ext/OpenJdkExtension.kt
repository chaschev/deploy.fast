package fast.dsl.ext

import fast.dsl.*
import fast.runtime.TaskContext
import mu.KLogging

data class OpenJdkConfig(
  var pack: String = "openjdk-8-jdk"
) : ExtensionConfig

class OpenJDKTasks(val ext: OpenJdkExtension, taskCtx: TaskContext) : NamedExtTasks(
  ext as DeployFastExtension<ExtensionConfig>, taskCtx
) {
  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process

  override suspend fun getStatus(): ExtensionTask {
    return ExtensionTask("getStatus", extension = extension) {
      val installed = ext.apt.tasks(this).listInstalled("openjdk")

      println("installed jdk packages: $installed")

      if (installed.isEmpty())
        ServiceStatus.notInstalled
      else
      ServiceStatus.installed
    }
  }

  suspend fun uninstall(): ExtensionTask {
    return ExtensionTask("getStatus", extension = extension) {

      val apt = ext.apt

      logger.info { "trying to delete old installed openjdk packages..." }

      for (i in 1..3) {
        val installed = apt.tasks(this).listInstalled("openjdk")

        if (installed.isEmpty()) {
          logger.info { "found 0 packages installed, finishing" }
        }

        logger.info { "#$i. found ${installed.size} old packages: ${installed.joinToString(",")}" }

        for (pack in installed) {
          logger.info { "removing $pack..." }
          apt.tasks(this).remove(pack)
        }
      }

      logger.info { "dpkg: trying to delete old openjdk packages..." }

      for (i in 1..3) {
        val installed = apt.tasks(this).dpkgListInstalled("openjdk")!!

        if (installed.isEmpty()) {
          logger.info { "found 0 packages installed, finishing" }
        }

        logger.info { "#$i. found ${installed.size} old packages: ${installed.joinToString(",")}" }

        for (pack in installed) {
          logger.info { "removing $pack..." }
          apt.tasks(this).dpkgRemove(pack.name)
        }
      }

      val installed1 = apt.tasks(this).listInstalled("openjdk")!!
      val installed2 = apt.tasks(this).dpkgListInstalled("openjdk")!!

      return@ExtensionTask TaskValueResult(installed1.isEmpty() && installed2.isEmpty())


    }
  }

  companion object : KLogging() {

  }
}

/**
 * This extension will generate vagrant project file.
 */
class OpenJdkExtension(
  config: (TaskContext) -> OpenJdkConfig
) : DeployFastExtension<OpenJdkConfig>(
  "openjdk", config
) {
  val apt = AptExtension({AptExtensionConfig()})

  override val tasks = {ctx:TaskContext -> OpenJDKTasks(this, ctx)}
}


