package fast.runtime

import fast.dsl.DeployFastApp
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.ext.OpenJdkConfig
import fast.dsl.ext.OpenJdkExtension
import fast.dsl.ext.VagrantConfig
import fast.dsl.ext.VagrantExtension

class CrawlersAppDeploy(app: AppContext) : DeployFastApp(app) {

  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension(app, {VagrantConfig(
    app.inventory["vm"].hosts
  )})

  val openJdk = OpenJdkExtension(app, {OpenJdkConfig(
    pack = "openjdk-8-jdk"
  )})

  companion object {
    fun dsl(app: AppContext) = DeployFastDSL.deployFast(CrawlersAppDeploy(app)) {
      info {
        name = "Vagrant Extension"
        author = "Andrey Chaschev"
      }

      globalTasksBeforePlay {
        task("update Vagrantfile") {
          ext.vagrant.tasks.updateFile()
        }
      }

      play {
        task {
          println("jdk installation status:" + ext.openJdk.getStatus())

          ok
        }
      }
    }
  }
}