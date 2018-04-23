package fast.dsl

class VagrantConfig(
  var mem: Int = 2048,
  var cpu: Int = 1,
  var linkedClone: Boolean = true,
  var user: String = "vargant",
  var password: String = "vagrant"
)

/**
 * This extension will generate vagrant project file.
 */
class VagrantExtension(): DeployFastExtension() {
  lateinit var config: VagrantConfig

  fun configure(
    block: VagrantConfig.() -> Unit){

    config = VagrantConfig().apply(block)
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


