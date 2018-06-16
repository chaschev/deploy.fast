package fast.sample

import fast.api.DeployFastApp
import fast.api.ITaskResult
import fast.api.ext.AptExtension
import fast.api.ext.VagrantConfig
import fast.api.ext.VagrantExtension
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL.Companion.createAppDsl
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.runtime.DeployFast
import fast.runtime.DeployFastScheduler
import fast.runtime.configDeployFastLogging
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.run
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.generic.instance
import java.io.File

class VpnFastApp : DeployFastApp<VpnFastApp>("vpn") {
  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })


  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      configDeployFastLogging()

      VpnAppDI

      val scheduler = DeployFastScheduler<VpnFastApp>()

      runBlocking {
        scheduler.doIt()
      }
    }

    fun dsl(): DeployFastAppDSL<VpnFastApp> {
      val app by DeployFast.FAST.instance<DeployFastApp<*>>()

      return createAppDsl(app as VpnFastApp) {
        val apt = AptExtension()


        info {
          name = "OpenVPN Installation"
          author = "Andrey Chaschev"
        }

        setupSsh {
          "vm" with {
            privateKey(it, "vagrant") {
              keyPath = "${"HOME".env()}/.vagrant.d/insecure_private_key"
            }
          }

          "other" with { privateKey(it) }
        }

        globalTasksBeforePlay {
          /*task("update_vagrantfile") {
            ext.vagrant.tasks(this).updateFile()
          }*/
        }

        play {
          task("install_vpn") {
            //            script {
//              sh("sudo apt-get install docker")
//            }.execute(ssh)
            var r: ITaskResult<*> = ok

            r *= apt.tasks(this).install("docker.io")

            r *= script {
              processInputWithMap(mapOf(
                  "Enter PEM pass phrase" to "pass",
                  "Confirm removal:" to "yes",
                  "Common Name" to "vpn",
                  "Enter pass phrase" to "pass"
                )
              )

              sh("export OVPN_DATA=/data/openvpn/")  //TODO; quotes don't work here

              sh("sudo docker run -v \$OVPN_DATA:/etc/openvpn --rm kylemanna/openvpn ovpn_genconfig -u udp://ovpn-host")
              sh("sudo docker run -v \$OVPN_DATA:/etc/openvpn --rm -it kylemanna/openvpn ovpn_initpki")
              sh("sudo docker run -v \$OVPN_DATA:/etc/openvpn -d -p 1195:1194/udp --cap-add=NET_ADMIN kylemanna/openvpn")
              sh("sudo docker run -v \$OVPN_DATA:/etc/openvpn --rm -it kylemanna/openvpn easyrsa build-client-full bb nopass")

              sh("sudo docker run -v \$OVPN_DATA:/etc/openvpn --rm kylemanna/openvpn ovpn_getclient bb > b1.ovpn")
            }.execute(ssh).toFast()

            val vpnConfigurationAsString = ssh.run("cat b1.ovpn")

            val vpnFixedConf = vpnConfigurationAsString.text().replace(
              """remote ([^\s]+) 1194 udp""".toRegex(), {
              "remote ${host.address} 1195 udp"
            })

            File(host.address + ".ovpn").writeText(vpnFixedConf)

            r.asBoolean()
          }
        }
      }
    }
  }
}
