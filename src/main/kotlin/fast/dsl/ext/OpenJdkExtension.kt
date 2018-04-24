package fast.dsl.ext

import fast.dsl.*
import fast.runtime.AppContext
import fast.runtime.TaskContext
import kotlinx.coroutines.experimental.runBlocking

data class OpenJdkConfig(
  var pack: String = "openjdk-8-jdk"
) : ExtensionConfig

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

  //todo: move to tasks
  /*override fun getStatus(): ServiceStatus {
    return runBlocking {
      val installed = apt.tasks().listInstalled("openjdk")!!

      if (installed.isEmpty()) return@runBlocking ServiceStatus.notInstalled

      println("installed jdk packages: $installed")

      ServiceStatus.installed
    }
  }*/

}


