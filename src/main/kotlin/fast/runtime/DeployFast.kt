package fast.runtime

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.api.ext.DepistranoConfigDSL.Companion.depistrano
import fast.dsl.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Group
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI.FAST
import fast.ssh.asyncNoisy
import fast.ssh.command.script.ScriptDsl
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.logger
import fast.ssh.run
import fast.ssh.runResult
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.*
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap


class AppContext {
  val runAt: String by FAST.instance(tag = "runAt")

  val inventory: Inventory by FAST.instance()

  val hosts: List<Host> = inventory.asOneGroup.getHostsForName(runAt)
  val globalMap = ConcurrentHashMap<String, Any>()

  /**
   * Party coordination could be done in a similar fashion.
   *
   * I.e. can await for an
   *  (taskKey, AtomicInteger) value,
   *  or a certain state of parties
   *  shared result state isCompleted() = true. Parties report to this result
   */

  suspend fun awaitKey(path: String, timeoutMs: Long = 600_000): Boolean {
    val startMs = System.currentTimeMillis()

    while(true) {
      if(globalMap.containsKey(path)) return true

      if(System.currentTimeMillis() - startMs > timeoutMs) return false

      delay(50)
    }
  }
  suspend fun <R> runOnce(path: String, block: suspend () -> R): Deferred<R> {
    return (globalMap.getOrDefault(path, {
      asyncNoisy {
        block()
      }
    }) as Deferred<R>)
  }
}

object DeployFastDI {
  var FAST = Kodein {
    bind<AppContext>() with singleton { AppContext() }
  }
    set(value) {
      FASTD = value.direct
      field = value
    }

  var FASTD = FAST.direct
}

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

  val depistrano = depistrano {
    checkout { ctx, ssh,folder,ref  ->
      val config = ctx.config

      with(config) {
        val checkedOut = ssh.files().exists("$srcDir/honey-badger")

        val refId = script {
          cd(srcDir)

          if (!checkedOut) {
            sh("git clone https://chaschev@bitbucket.org/chaschev/honey-badger.git")
          } else {
            cd("honey-badger")

            sh("git pull")
          }

          capture("revisionCapture") {
            sh("git rev-parse --verify HEAD")
          }
        }.execute(ssh)["revisionCapture"]!!.text!!.toString()

        if(!checkedOut) {
          ssh.runResult("cd $srcDir && git clone https://chaschev@bitbucket.org/chaschev/honey-badger.git")
        } else {
          ssh.runResult("cd $srcDir/honey-badger && git pull")
        }

        VCSUpdateResult(refId, refId.substring(0, 6))
      }
    }

    build {
      ssh.runResult("gradle build")

      val buildResult = script {
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
    fun dsl(): DeployFastAppDSL<CrawlersFastApp> {
      val app by FAST.instance<DeployFastApp<*>>()

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
              logger.debug { pwd.text()}
            }

            val duration = Duration.between(Instant.now(), startedAt)

            logger.info { "finished in $duration, which is ${duration.toMillis() / times}ms per operation" }

            TaskResult.ok
          }

          task("install_java") {
            ext.openJdk.tasks(this).installJava()
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


object CrawlersAppDI {

  init {
    DeployFastDI.FAST = Kodein {
      extend(FAST)

      bind<Inventory>() with singleton {
        Inventory(
          listOf(
            Group(
              name = "vpn",
              hosts = listOf(
                Host("vpn1"),
                Host("vpn2")
              )
            ),
            Group(
              name = "vm",
              hosts = listOf(
                Host("192.168.5.10")
              )
            )
          )
        ).init()
      }

      bind<DeployFastApp<*>>() with singleton { CrawlersFastApp() }

      bind("dsl") from singleton { CrawlersFastApp.dsl() }

      bind("runAt") from singleton { "vm" }
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val scheduler = DeployFastScheduler<CrawlersFastApp>()

    runBlocking {
      scheduler.doIt()
    }
  }
}

