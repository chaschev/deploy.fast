package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.failed
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.ssh.run
import fast.ssh.runAndWait
import fast.ssh.runResult
import kotlinx.coroutines.experimental.delay
import mu.KLogging

typealias SystemdTaskContext = ChildTaskContext<SystemdExtension, SystemdConfig>


/*
 */
class SystemdExtension(
  config: (SystemdTaskContext) -> SystemdConfig
) : DeployFastExtension<SystemdExtension, SystemdConfig>(
  "systemd", config
) {
  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    SystemdTasks(this@SystemdExtension, parentCtx)
  }
}

class SystemdTasks(ext: SystemdExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<SystemdExtension, SystemdConfig>(ext, parentCtx) {

  val installService by extensionTask {
    logger.info { "installing systemd service ${config.name}.service" }

    val confString = SystemdTemplate(config).generate()

    val tempServiceFile = "/tmp/${config.name}.service"

    ssh.files(sudo = true).remove(config.servicePath, tempServiceFile)
    ssh.files(sudo = true).writeToString(tempServiceFile, confString)

    ssh.run("sudo cp $tempServiceFile ${config.servicePath}")
    ok
  }

  val isActive by extensionTask {
    ssh.runResult("sudo systemctl is-active ${config.name}").toFast()
  }

  val isEnabled by extensionTask {
    ssh.runAndWait("sudo systemctl is-enabled ${config.name}").toFast()
  }

  val start by extensionTask {
    ssh.runAndWait("sudo systemctl start ${config.name}").toFast()
  }

  suspend fun startAndAwait(startTimeoutSec: Int = 120, aliveTimeoutSec:Int = 20) =
    extensionFun("startAndAwait", {
    start()

    val startedMs = System.currentTimeMillis()

    var active: Boolean = false

    while(true) {
      val status = isActive().text()
      active = status == "active"
      if(active) break

      val passedMs = System.currentTimeMillis() - startedMs
      if(passedMs > startTimeoutSec * 1000) break

      logger.info { "awaiting ${config.name} to come up [${passedMs / 1000}/$startTimeoutSec]s..." }

      delay(2000)
    }

    if(!active) return@extensionFun failed("timeout while waiting for service to come up")

    while(true) {
      active = isActive().text() == "active"
      if(!active) break

      val passedMs = System.currentTimeMillis() - startedMs
      if(passedMs > aliveTimeoutSec * 1000) break

      logger.info { "awaiting ${config.name} to stay alive [${passedMs / 1000}/$aliveTimeoutSec]s..." }

      delay(2000)
    }

    if(!active) {
      logger.info { "service  ${config.name} crashed after timeout. Retrieving logs..."  }
      val logs = logs().value
      return@extensionFun failed("service crashed after timeout, logs: $logs")
    }

    ok
  })

  val stop by extensionTask {
    ssh.runAndWait("sudo systemctl stop ${config.name}").toFast()
  }

  val status by extensionTask {
    ssh.runResult("sudo systemctl status ${config.name}").toFast().mapValue { it.console.stdout.toString() }
  }

  val logs by extensionTask {
    ssh.runResult("sudo journalctl -u ${config.name} --no-pager | tail -n 20").toFast().mapValue { it.console.stdout.toString() }
  }

//  suspend fun installService() = installServiceTask()
//  suspend fun logs() = logs.play(extCtx)


  companion object : KLogging()
}

class SystemdConfig(
  val name: String,
  val exec: String,
  val directory: String = ".",
  val description: String = "",
  val user: String? = null,
  val pidfile: String? = null,
  val env: Map<String, String> = emptyMap()
) : ExtensionConfig {
  val servicePath: String = "/etc/systemd/system/$name.service"
}


//TODO: copy defaults from SystemdConfig

data class SystemdHost(
  val host: Host,
  val hostname: String = host.name,
  val ip: String = host.address,
  val box: String = "ubuntu/xenial64",
  val netmask: String = "255.255.255.0",
  val memory: Int = 1024,
  val cpu: Int = 1,
  val linkedClone: Boolean = true,
  val user: String = "vargant",
  val password: String = "systemd"
)





