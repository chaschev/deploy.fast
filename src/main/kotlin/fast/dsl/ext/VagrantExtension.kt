package fast.dsl.ext

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Host
import fast.runtime.AppContext
import fast.runtime.TaskContext
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


class VagrantTasks : NamedExtTasks() {
  fun updateFile(): TaskResult {
    val vagrantFile = File("Vagrantfile")

    val text = nullForException {vagrantFile.readText()}

    if(text == null || !text.substring(200).contains("Managed by ")) {
      VagrantTemplate(context.config as VagrantConfig).writeToFile(vagrantFile)
    } else {
      throw Exception("can't write to $vagrantFile: it already exists and not ours!")
    }

    return ok
  }

  /* Don't really implement this */
  fun vagrantUp(): TaskResult {
    TODO()
  }

//  val config by lazy { _config as VagrantConfig}
}

data class VagrantHost(
  val host: Host,
  val hostname: String = host.name,
  val ip: String = host.address,
  val box: String = "ubuntu64/xenial",
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
) : DeployFastExtension<VagrantConfig>(app, config) {
  override val tasks = VagrantTasks()
}


