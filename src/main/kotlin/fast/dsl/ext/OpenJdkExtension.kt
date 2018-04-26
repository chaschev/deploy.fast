package fast.dsl.ext

import fast.dsl.*
import fast.runtime.TaskContext

data class OpenJdkConfig(
  var pack: String = "openjdk-8-jdk"
) : ExtensionConfig

class OpenJDKTasks(val ext: OpenJdkExtension) : NamedExtTasks(ext as DeployFastExtension<ExtensionConfig>) {
  open fun getInstalledState(): Boolean = TODO()

  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process

  override suspend fun getStatus(): ExtensionTask {
    return ExtensionTask("getStatus", extension = extension) {
      val installed = ext.apt.tasks(it).listInstalled("openjdk").play(it) as TaskValueResult<Set<String>?>

      println("installed jdk packages: ${installed.value}")

      if (installed.value!!.isEmpty())
        ServiceStatus.notInstalled
      else
      ServiceStatus.installed
    }
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

  override val tasks = {_:TaskContext -> OpenJDKTasks(this)}
}


