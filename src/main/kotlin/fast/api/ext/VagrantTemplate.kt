package fast.api.ext

import java.io.File

class VagrantTemplate(
  val config: VagrantConfig
) {
  fun generate() = configureToString(config)

  fun writeToFile(file: File) = file.writeText(generate())

  companion object {
    fun configureToString(config: VagrantConfig): String =
      with(config) {
        return """
# Managed by Deploy.Fast

Vagrant.configure(2) do |config|
    config.ssh.forward_agent = true
    config.ssh.insert_key = false
    config.vm.synced_folder ".", "/vagrant", disabled: true

    ${hostConfigs.joinToString("\n") {
          with(it) {
            """
      config.vm.define "$hostname" do |host|
        host.vm.box = "$box"
        host.vm.network "private_network", ip: "$ip", :netmask => "$netmask"
        host.vm.hostname = "$hostname"
        host.vm.provider "virtualbox" do |vb|
          vb.gui    = false
          vb.memory = "$memory"
          vb.cpus   = $cpu
          vb.linked_clone = $linkedClone
        end
      end
"""
          }
        }
        }

end
"""
      }
  }

}