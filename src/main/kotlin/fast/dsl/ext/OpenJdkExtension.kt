package fast.dsl.ext

import fast.dsl.*
import kotlinx.coroutines.experimental.runBlocking

data class OpenJdkConfig(
  var pack: String = "openjdk-8-jdk"
)

/**
 * This extension will generate vagrant project file.
 */
class OpenJdkExtension() : DeployFastExtension() {
  lateinit var config: OpenJdkConfig

  val apt = AptExtension()

  fun configure(
    block: OpenJdkConfig.() -> Unit): OpenJdkExtension {

    config = OpenJdkConfig().apply(block)

    return this
  }

  override fun getStatus(): ServiceStatus {
    return runBlocking {
      val installed = apt.tasks.listInstalled("openjdk")!!

      if (installed.isEmpty()) return@runBlocking ServiceStatus.notInstalled

      println("installed jdk packages: $installed")

      ServiceStatus.installed
    }
  }

  companion object {
    fun dsl() = DeployFastDSL.deployFast(VagrantExtension()) {
      info {
        name = "Vagrant Extension"
        author = "Andrey Chaschev"
      }

      beforeGlobalTasks {

      }
    }

  }
}


