package fast.runtime

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.api.ext.DepistranoConfigDSL.Companion.depistrano
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.toFast
import fast.ssh.command.script.ScriptDsl
import fast.ssh.logger
import fast.ssh.run
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

  val cassandra = CassandraExtension({
    CassandraConfig(
      clusterName = "deploy.fast.cluster",
      _hosts = app.hosts
    )
  })

  val depistrano = depistrano {
    ctx.session.ssh.user()
    projectDir = "${ctx.home}/crawlers"
//    projectName = "honey-badger"
    projectName = "deploy.fast"

    checkout {
      url = "https://github.com/chaschev/deploy.fast.git"
//      url = "https://chaschev@bitbucket.org/chaschev/honey-badger.git"
    }

    build {
      val buildResult = ScriptDsl.script {
        settings { abortOnError = false }

        cd("$srcDir/$projectName")
//        sh("rm -rf build/*")
        sh("gradle build --console plain")
      }.execute(ssh).toFast()

      buildResult.abortIfError {"gradle build error:\n ${GradleExtension.extractError(buildResult)}" }

      val jar = ssh.files().ls("$srcDir/$projectName/build/libs").find { it.name.endsWith(".jar") }!!

      val artifacts = buildResult.mapValue { listOf(jar.path) }

      artifacts
    }

    distribute {
      /* auto-distribution of build results */
    }

    execute {
      // todo: install a service and make sure it is running
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      configDeployFastLogging()

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
              val pwd = ssh.run("ls /home/vagrant/crawlers/releases")
              logger.debug(host) { pwd.text() }
            }

            val duration = Duration.between(Instant.now(), startedAt)

            logger.info(host) { "finished in $duration, which is ${duration.toMillis() / times}ms per operation" }

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
