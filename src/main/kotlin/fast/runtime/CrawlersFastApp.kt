package fast.runtime

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.api.ext.DepistranoConfigDSL.Companion.depistrano
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.toFast
import fast.ssh.command.ConsoleLogging.SSH_OUT_MARKER
import fast.ssh.command.script.ScriptDsl
import fast.ssh.logger
import fast.ssh.run
import kotlinx.coroutines.experimental.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.ThreadContext
import org.kodein.di.generic.instance
import java.time.Duration
import java.time.Instant
import org.apache.logging.log4j.core.appender.FileAppender
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.routing.RoutingAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.Configuration


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
        settings { abortOnError = true }

        cd("$srcDir/$projectName")
        sh("rm -rf build/*")
        sh("gradle build --console plain")
      }.execute(ssh)

      val jar = ssh.files().ls("$srcDir/$projectName/build/libs").find { it.name.endsWith(".jar") }!!

      val artifacts = buildResult.toFast().mapValue { listOf(jar.path) }

      artifacts
    }

    distribute {
      /* auto-distribution of build results */
    }

    execute {
      // todo: install a service and make sure it is running
    }
  }

  class FixedFileAppender : FileAppender.Builder<FixedFileAppender>() {
    companion object {
      fun newFileAppend(): FixedFileAppender {
        return FixedFileAppender().asBuilder()
      }
    }
  }

  class FixedRoutingAppender : RoutingAppender.Builder<FixedRoutingAppender>() {
    companion object {
      fun newRoutingAppend(): FixedRoutingAppender {
        return FixedRoutingAppender().asBuilder()
      }
    }
  }


  //TODO for logging
  // TODO text output into separate files
  // TODO sessions log with a session marker - make a project requirement if needed
  // TODO aggregated log for fast search
  // TODO try ctx.info
  // TODO try KLogging.info(marker, msg)
  object TestLogging {
    @JvmStatic
    fun main(args: Array<String>) {
      val ctx = LogManager.getContext(false) as LoggerContext
      val config = ctx.configuration

      val layout = PatternLayout.newBuilder()
        .withConfiguration(config)
        .withPattern("%d{HH:mm:ss.SSS} %level %msg%n")
        .build()



      val appender = FixedFileAppender.newFileAppend()
        .setConfiguration(config)
        .withName("ssh.out.appender")
        .withLayout(layout)
//        .withFilter(
//          MarkerFilter.createFilter(
//
//          )
//        )
        .withFileName("logs/java.log")
        .build()

      appender.start()
      config.addAppender(appender)

      ThreadContext.put("threadName", Thread.currentThread().name);


      config.addLogger(ctx, appender)

      val logger1 = LogManager.getLogger("test")
      val warnLevelLogger = LogManager.getLogger("net.schmizz")

      logger1.info("hi info")
      logger1.debug("hi debug")

      warnLevelLogger.debug("that is disabled in log4j.xml")
      warnLevelLogger.debug("that is disabled in log4j.xml")

      val loggerSshOut = LogManager.getLogger("ssh.out")

      loggerSshOut.warn(SSH_OUT_MARKER, "ssh out")
//      loggerSshOut.warn(IP1_MARKER, "ip1 out")
//      loggerSshOut.warn(IP2_MARKER, "ip2 out")
    }

    fun Configuration.addLogger(ctx: LoggerContext, appender: FileAppender) {
      val ref = AppenderRef.createAppenderRef(appender.name, null, null)
      val refs = arrayOf(ref)

      addLogger("programmaticLogger", LoggerConfig
        .createLogger(false, Level.INFO, "programmaticLogger", "false", refs, null, this, null)
        .apply {
          addAppender(appender, null, null)
        })

      ctx.updateLoggers()
    }
  }

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