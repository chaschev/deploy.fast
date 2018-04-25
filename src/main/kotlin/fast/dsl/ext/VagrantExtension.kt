package fast.dsl.ext

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Host
import fast.runtime.AppContext
import fast.runtime.TaskContext
import mu.KLogging
import java.io.File

inline fun <T> nullForException(
  onError: ((Throwable) -> Unit) = { _ -> },
  block: () -> T): T? {

  return try {
    block.invoke()
  } catch (e: Throwable) {
    onError.invoke(e)
    null
  }
}


class VagrantTasks(extension: DeployFastExtension<ExtensionConfig>) : NamedExtTasks(extension) {
  fun updateFile(): Task {
    //TODO: finish
    /*
    updateFile requires
      extension
      config
      if it is called from outside, a new task must be created
    need to pass proper extension to a task
    coordinate tasks and contexts and extensions
     */
    return ExtensionTask("updateFile", null, extension, { ctx ->
      logger.info { "updating Vagrantfile" }
      val vagrantFile = File("Vagrantfile")

      val text = nullForException {vagrantFile.readText()}

      if(text == null || !text.substring(200).contains("Managed by ")) {
        VagrantTemplate(ctx.config as VagrantConfig).writeToFile(vagrantFile)
      } else {
        throw Exception("can't write to $vagrantFile: it already exists and not ours!")
      }

       ok
    })

  }

  /* Don't really implement this */
  fun vagrantUp(): ITaskResult {
    TODO()
  }

  companion object : KLogging() {

  }

//  val config by lazy { _config as VagrantConfig}
}

//TODO: copy defaults from VagrantConfig

data class VagrantHost(
  val host: Host,
  val hostname: String = host.name,
  val ip: String = host.address,
  val box: String = "ubuntu/xenial64",
  val netmask: String = "255.255.255.0",
  val memory: Int = 1024,
  val cpu: Int = 1,
  val linkedClone: Boolean = true,
  val user: String = "vargant",
  val password: String = "vagrant"
)

class VagrantConfig(
  val hosts: List<Host> = ArrayList(),
  val hostConfigs: List<VagrantHost> = hosts.map { VagrantHost(it) }
) : ExtensionConfig

/**
 * This extension will generate vagrant project file.
 */
class VagrantExtension(
  app: AppContext,
  config: (TaskContext) -> VagrantConfig
) : DeployFastExtension<VagrantConfig>("vagrant", app, config) {
  override val tasks: (TaskContext) -> VagrantTasks = {VagrantTasks(this as DeployFastExtension<ExtensionConfig>)}
}


