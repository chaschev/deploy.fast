package fast.sample

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.toFast
import fast.runtime.DeployFastDI
import fast.runtime.DeployFastScheduler
import fast.runtime.configDeployFastLogging
import fast.ssh.command.script.ScriptDsl
import fast.ssh.logger
import fast.ssh.run
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.generic.instance
import java.time.Duration
import java.time.Instant

class CloudOneFastApp : DeployFastApp<CloudOneFastApp>("cloudOne") {
  /* TODO: convert to method invocation API */
  val vagrant = VagrantExtension({
    VagrantConfig(app.hosts)
  })

  val openJdk = OpenJdkExtension({
    OpenJdkConfig(pack = "openjdk-8-jdk")
  })

  val gradle = GradleExtension({ GradleConfig(version = "4.7") })

  val cassandra = CassandraExtension({
    CassandraConfig(
      clusterName = "deploy.fast.cluster",
      _hosts = app.hosts
    )
  })

  val depistrano = DepistranoConfigDSL.depistrano {
    projectDir = "${ctx.home}/cloudOne"
//    projectName = "honey-badger"
    projectName = "cloudOne"

    checkout {
      url = "https://github.com/chaschev/deploy.fast.git"
//      url = "https://chaschev@bitbucket.org/chaschev/honey-badger.git"
    }

    build {
      val buildResult = ScriptDsl.script {
        settings { abortOnError = false }

        cd("$srcDir/honey-badger/honey-badger3")
        sh("gradle build --console plain")
        capture("version") {
          sh("java -cp build/libs/* version")
        }
      }.execute(ssh)

      logger.info(host) { "finished building app, version: " + buildResult["version"] }

      val buildFastResult = buildResult.toFast()

      buildFastResult.abortIfError { "gradle build error:\n ${GradleExtension.extractError(buildFastResult)}" }

      val jar = ssh.files().ls("$srcDir/$projectName/build/libs").find { it.name.endsWith(".jar") }!!

      val artifacts = buildFastResult.mapValue { listOf(jar.path) }

      artifacts
    }

    distribute {
      ScriptDsl.script {
        sh("java -cp build/libs/* getJars")
      }
    }

    execute {
      // todo: install a service and make sure it is running
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      configDeployFastLogging()

      CloudOneAppDI

      val scheduler = DeployFastScheduler<CloudOneFastApp>()

      runBlocking {
        scheduler.doIt()
      }
    }

    fun dsl(): DeployFastAppDSL<CloudOneFastApp> {
      val app by DeployFastDI.FAST.instance<DeployFastApp<*>>()

      return DeployFastDSL.createAppDsl(app as CloudOneFastApp) {
        info {
          name = "Vagrant Extension"
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
              val pwd = ssh.run("ls /home/vagrant/cloudOne/releases")
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