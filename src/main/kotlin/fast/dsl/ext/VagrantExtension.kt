package fast.dsl.ext

import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Host
import fast.runtime.AnyTaskContext
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

typealias VagrantTaskContext = ChildTaskContext<VagrantExtension, VagrantConfig>

class VagrantExtension(
  config: (VagrantTaskContext) -> VagrantConfig
) : DeployFastExtension<VagrantExtension, VagrantConfig>(
  "vagrant", config
) {
  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    VagrantTasks(this@VagrantExtension, parentCtx)
  }
}


class VagrantTasks(ext: VagrantExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<VagrantExtension, VagrantConfig>(ext, parentCtx) {

  suspend fun updateFile(): ITaskResult<Boolean> {
    val task = LambdaTask("updateFile", extension) {
      logger.info { "updating Vagrantfile" }
      val vagrantFile = File("Vagrantfile")

      val text = nullForException { vagrantFile.readText() }

      if (text == null || !text.substring(200).contains("Managed by ")) {
        //probable IDEA bug: extension here is AnyExtension

        VagrantTemplate(extension.config(it as VagrantTaskContext))
          .writeToFile(vagrantFile)
      } else {
        throw Exception("can't write to $vagrantFile: it already exists and not ours!")
      }

      ok
    }

    return task.play(extCtx)
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


