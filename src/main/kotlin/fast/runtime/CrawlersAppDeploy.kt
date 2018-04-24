package fast.runtime

import fast.dsl.DeployFastApp
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.ext.OpenJdkConfig
import fast.dsl.ext.OpenJdkExtension
import fast.dsl.ext.VagrantConfig
import fast.dsl.ext.VagrantExtension

class CrawlersAppDeploy(app: AppContext) : DeployFastApp("crawlers", app) {

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
        task("update_vagrantfile") {
          //TODO vagrant extension context should be created here
          //TODO tasks(...) should actually prepare a new task with vagrant config & context
          //TODO remove context from tasks()
          //TODO updateFile() should return a wrapper, which will create it's own context + context based on
          //normally, new extension context are not initialized
          //"Vagrant extension": "we want to create our own context and then run tasks
          ext.vagrant.tasks(this).updateFile().play(this)
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