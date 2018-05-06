package fast.runtime

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.toFast
import fast.ssh.command.script.ScriptDsl
import fast.ssh.files.exists
import fast.ssh.logger
import fast.ssh.run
import fast.ssh.runResult
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.generic.instance
import java.time.Duration
import java.time.Instant


fun String.env() = System.getenv(this)

class CrawlersFastApp : DeployFastApp<CrawlersFastApp>("crawlers") {


  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })

  val openJdk = OpenJdkExtension({
    OpenJdkConfig(
      pack = "openjdk-8-jdk"
    )
  })

  val gradle = GradleExtension({ GradleConfig(version = "4.7") })

  val depistrano = DepistranoConfigDSL.depistrano {
    ctx.session.ssh.user()
    projectDir = "${ctx.home}/crawlers"


    checkout { ctx, ssh, folder, ref ->
      val config = ctx.config

      with(config) {
        val checkedOut = ssh.files().exists("$srcDir/honey-badger")

        val refId = ScriptDsl.script {
          cd(srcDir)

          capture {
            processConsole = { console, newText ->
              if (newText.contains("Password for ")) {
                val password = ctx.getStringVar("git.password")
                console.writeln(password)
              }
            }

            if (!checkedOut) {
              sh("git clone https://chaschev@bitbucket.org/chaschev/honey-badger.git")
              cd("honey-badger")
            } else {
              cd("honey-badger")

              sh("git pull")
            }
          }

          capture("revisionCapture") {
            sh("git rev-parse --verify HEAD")
          }
        }.execute(ssh)["revisionCapture"]!!.text!!.toString()

        logger.info { "checked out revision $refId" }

        VCSUpdateResult(refId, refId.substring(0, 6))
      }
    }

    build {
      ssh.runResult("gradle build")

      val buildResult = ScriptDsl.script {
        cd("$srcDir/honey-badger")
        sh("rm build/*.jar")
        sh("gradle build")
      }.execute(ssh)

      val jar = ssh.files().ls("$srcDir/honey-badger/build").find { it.name.endsWith(".jar") }!!

      buildResult.toFast().mapValue { listOf(jar.path) }
    }

    //todo select node and synchronize
    distribute {
      // todo: call extension
    }

    execute {
      // todo: install service and make sure it is running
    }
  }

  val cassandra = CassandraExtension({
    CassandraConfig("deploy.fast.cluster", app.hosts)
  })

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      CrawlersAppDI

      val scheduler = DeployFastScheduler<CrawlersFastApp>()

      runBlocking {
        scheduler.doIt()
      }
    }


    fun dsl(): DeployFastAppDSL<CrawlersFastApp> {
      val app by DeployFastDI.FAST.instance<DeployFastApp<*>>()

      return DeployFastDSL.createAppDsl(app as CrawlersFastApp) {
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

          "other" with { privateKey(it) }
        }

        globalTasksBeforePlay {
          task("update_vagrantfile") {
            ext.vagrant.tasks(this).updateFile()
          }
        }

        play {
          task("check_java_and_speed_test") {
            println("jdk installation status:" + ext.openJdk.tasks(this).getStatus())

            val startedAt = Instant.now()
            val times = 3

            repeat(times) {
              val pwd = ssh.run("pwd; cd /etc; pwd")
              logger.debug { pwd.text() }
            }

            val duration = Duration.between(Instant.now(), startedAt)

            logger.info { "finished in $duration, which is ${duration.toMillis() / times}ms per operation" }

            TaskResult.ok
          }

          task("install_java") {
            ext.openJdk.tasks(this).installJava()
            ext.gradle.tasks(this).install()
          }

          task("depistrano") {
            with(extension.depistrano.tasks(this)) {
              installRequirements()
              deploy()
            }

          }

//          task("install_cassandra") {
//            ext.cassandra.tasks(this).install()
//          }
        }
      }
    }
  }
}