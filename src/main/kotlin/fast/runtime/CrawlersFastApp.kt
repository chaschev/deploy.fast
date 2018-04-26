package fast.runtime

import fast.dsl.DeployFastApp
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.ext.OpenJdkConfig
import fast.dsl.ext.OpenJdkExtension
import fast.dsl.ext.VagrantConfig
import fast.dsl.ext.VagrantExtension
import org.kodein.di.generic.instance

class CrawlersFastApp(app: AppContext) : DeployFastApp("crawlers") {

  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })

  val openJdk = OpenJdkExtension(app, {
    OpenJdkConfig(
      pack = "openjdk-8-jdk"
    )
  })

  companion object {
    val app: AppContext by DeployFastDI.FAST.instance()

    fun dsl(): DeployFastAppDSL<CrawlersFastApp> {
      return DeployFastDSL.createAppDsl(CrawlersFastApp(app)) {
        info {
          name = "Vagrant Extension"
          author = "Andrey Chaschev"
        }

        ssh {
          "vm" with {
            privateKey(it, "vagrant") {
              keyPath = "${"HOME".env()}/.vagrant.d/insecure_private_key"
            }
          }

          "other" with { privateKey(it)  }
        }

        globalTasksBeforePlay {
          task("update_vagrantfile") {
            ext.vagrant.tasks(this).updateFile().play(this)
          }
        }

        play {
          task("check_java") {
            println("jdk installation status:" + ext.openJdk.tasks(this).getStatus().play(this))

            ok
          }
        }
      }
    }
  }
}


 fun String.env() = System.getenv(this)