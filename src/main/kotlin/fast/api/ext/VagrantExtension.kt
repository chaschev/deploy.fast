package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Host
import fast.lang.nullForException
import fast.runtime.TaskContext
import mu.KLogging
import java.io.File

typealias VagrantTaskContext = ChildTaskContext<VagrantExtension, VagrantConfig>

/**
 * This extension will generate vagrant project file.
 */
class VagrantExtension(
  config: (TaskContext<*, *, *>) -> VagrantConfig
) : DeployFastExtension<VagrantExtension, VagrantConfig>(
  "vagrant", config
) {
  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    VagrantTasks(this@VagrantExtension, parentCtx)
  }
}


class VagrantConfig(
  val hosts: List<Host> = ArrayList(),
  val hostConfigs: List<VagrantHost> = hosts.map { VagrantHost(it) }
) : ExtensionConfig


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


class VagrantTasks(ext: VagrantExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<VagrantExtension, VagrantConfig>(ext, parentCtx) {

  suspend fun updateFile() = extensionFun("updateFile") {
      logger.info { "updating Vagrantfile" }

      val config = extension.config(this)

      val vagrantFile = File("Vagrantfile")

      val text = nullForException { vagrantFile.readText() }

      if (text == null || !text.substring(200).contains("Managed by ")) {
        VagrantTemplate(config).writeToFile(vagrantFile)
        ok
      } else {
        throw Exception("can't write to $vagrantFile: it already exists and not ours!")
      }
  }



  companion object : KLogging()
}


