package fast.dsl.ext

import fast.dsl.*
import fast.runtime.AppContext
import fast.runtime.TaskContext
import kotlinx.coroutines.experimental.runBlocking

data class OpenJdkConfig(
  var pack: String = "openjdk-8-jdk"
) : ExtensionConfig

class OpenJDKTasks(extension: OpenJdkExtension) : NamedExtTasks(extension as DeployFastExtension<ExtensionConfig>) {
  open fun getInstalledState(): Boolean = TODO()

  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process

  val ext = extension

  override suspend fun getStatus(): ExtensionTask {
    return ExtensionTask("getStatus", extension = extension) {
      val installed = ext.apt.tasks(it).listInstalled("openjdk")!!

      if (installed.isEmpty()) ServiceStatus.notInstalled

      println("installed jdk packages: $installed")

      ServiceStatus.installed
    }

  }
}

/**
 * This extension will generate vagrant project file.
 */
class OpenJdkExtension(
  app: AppContext,
  config: (TaskContext) -> OpenJdkConfig
) : DeployFastExtension<OpenJdkConfig>(
  "openjdk", app, config
) {
  val apt = AptExtension(app, {AptExtensionConfig()})
}


