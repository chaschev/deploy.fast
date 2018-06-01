package fast.sample

import fast.api.DeployFastApp
import fast.api.ext.*
import fast.api.ext.DepistranoConfigDSL.Companion.depistrano
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult
import fast.dsl.toFast
import fast.inventory.Host
import fast.inventory.Inventory
import fast.runtime.DeployFastDI
import fast.runtime.DeployFastDI.FAST
import fast.runtime.DeployFastScheduler
import fast.runtime.configDeployFastLogging
import fast.sample.KikkitClusterDsl.Companion.cluster
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.logger
import fast.ssh.run
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.kodein.di.generic.instance
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/*
 * Subcluster X:
 *
 * Instance1  Instance2   Instance3
 *      U           U
 *                  F           F
 *      M           M
 *
 *  Each of the instance can respond to any of the calls
 *  The DB is shared
 *  Each instance has data of M service
 *  No M for Instance 3 means: rateLimit = 0 + it cannot handle local keys
 *  Does it make any sense?
 *  It is difficult to say that
 *
 *  U = User Service
 *  F = Feed Service
 *  M = Messaging Service
 *
 *  Which means that each instance of the cluster can
 *   inject U, F or M service
 *
 *  Managing Kikkit:
 *
 *   1) submit configuration change
 *   2) approve on the server or apply directly if it is not-interactive and no problems found
 *   3) what can be changed:
 *      up/down instances
 *      change hazelcast settings
 *      change rate limiting
 *       how to rate limit remote hazelcast call?
 *        "easy": if system is fine then direct call; else reply with a job
 *        "simple":
 *          while(true) { if(resourceManager.isMyTurn(this)) { doJob() } }
 *          todo: think how resource manager keeps track of system load and accepts new jobs
 *      run data update
 *      change rights
 *
 */


interface KikkitService<I : KikkitInstance> {
  val name: String
  val instance: I

  fun up()
  fun down()
}


open class KikkitInstance(
  var host: Host = Host.local,
  var port: Int = 0
) {
  val services = arrayListOf<String>()

}

class CrawlersInstance(
  host: Host,
  port: Int
) : KikkitInstance(
  host, port
) {
  val markets = arrayListOf<String>()
}

class KikkitClusterDsl(name: String) {
  val subclusters = arrayListOf<KikkitSubcluster<*>>()
  val inventory by FAST.instance<Inventory>()
  val hosts by lazy { inventory.getHostsForName(name) }

  inline fun subcluster(name: String, block: KikkitSubcluster<KikkitInstance>.() -> Unit) {
    KikkitSubcluster(name, KikkitInstance::class).apply(block)
  }

  inline fun <reified I : KikkitInstance> subclusterCustom(name: String, block: KikkitSubcluster<I>.() -> Unit) {
    KikkitSubcluster(name, I::class).apply(block)
  }


  companion object {
    fun cluster(name: String, block: KikkitClusterDsl.() -> Unit) {
      KikkitClusterDsl(name).apply(block)
    }
  }
}

class KikkitSubcluster<I : KikkitInstance>(
  val name: String,
  val instanceClass: KClass<I>
) {
  val map = HashMap<Host, ArrayList<I>>()

  // block: (I.() -> Unit)? = null
  infix fun List<Host>.map(ports: IntRange): List<ArrayList<I>> {
    return flatMap { host -> ports.map { host to it } }
      .map { (host, port) ->
        val instances = map.getOrPut(host, { ArrayList() })

        val instance = instances.find { it.port == port }
          ?: instanceClass.createInstance()

        instance.host = host
        instance.port = port

        instances
      }
  }

  infix fun List<ArrayList<I>>.apply(block: I.() -> Unit) {
    forEach { it.apply { block } }
  }

  companion object {
    val dsl = listOf(
      cluster("dev") {
        subcluster("main") {
          hosts map 7000..7001 apply {

          }
        }

        subcluster("webapp") {
          hosts map 9000..9001
        }

        subclusterCustom<CrawlersInstance>("crawlers") {
          hosts map 7002..7005 apply {
            markets += when (port) {
              7002 -> listOf("binance", "kucoin")
              7003 -> listOf("bitfinex")
              7004 -> listOf("bitstamp")
              else -> emptyList()
            }
          }
        }
      }

    )
  }
}


class KikkitFastApp : DeployFastApp<KikkitFastApp>("kikkit") {
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

  val depistrano = depistrano {
    projectDir = "${ctx.home}/kikkit"
//    projectName = "honey-badger"
    projectName = "kikkit"

    checkout {
      url = "https://github.com/chaschev/deploy.fast.git"
//      url = "https://chaschev@bitbucket.org/chaschev/honey-badger.git"
    }

    build {
      val buildResult = script {
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
      script {
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

      KikkitAppDI

      val scheduler = DeployFastScheduler<KikkitFastApp>()

      runBlocking {
        scheduler.doIt()
      }
    }

    fun dsl(): DeployFastAppDSL<KikkitFastApp> {
      val app by DeployFastDI.FAST.instance<DeployFastApp<*>>()

      return DeployFastDSL.createAppDsl(app as KikkitFastApp) {
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
              val pwd = ssh.run("ls /home/vagrant/kikkit/releases")
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

object TestPercentile {
  @JvmStatic
  fun main(args: Array<String>) {
    val p = Percentile()
    val data = doubleArrayOf(
      -10.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      6.0, 6.0, 6.0, 6.0,
      1000.0
    )

    p.data = data

    println(p.evaluate(95.0))


  }
}

object TestExecutor {
  @JvmStatic
  fun main(args: Array<String>) {
    val cutor = Executors.newFixedThreadPool(10000)

    repeat(100000) { jobIndex ->
      cutor.submit(Callable {
        runBlocking {
          repeat(10) {
            println("hi from $jobIndex: $it")
            delay(500)
          }
        }
      })
    }

    cutor.shutdown()
    cutor.awaitTermination(30, TimeUnit.SECONDS)
  }
}


//50k couroutines lead to 2 loaded at 100% on Macbook Pro '17
object CoroutineMassAwait {
  @JvmStatic
  fun main(args: Array<String>) {
    val startedAtMs = System.currentTimeMillis()
    runBlocking {
      (1..50000).map { jobIndex ->
        async {
          repeat(60) {
            if (jobIndex == 50) {
              println("hi from $jobIndex: $it")
              println("${System.currentTimeMillis() - startedAtMs} vs ${(it + 1) * 500}")
            }
            delay(6000)
          }
        }
      }.forEach {
        it.join()
      }
    }
  }
}


object CoroutineChannelsAwait {
  @JvmStatic
  fun main(args: Array<String>) {
    val startedAtMs = System.currentTimeMillis()

    runBlocking {
      val pairs = (0 until 1000000).map { jobIndex ->
        val channel = Channel<Int>()

        async {
            if (jobIndex == 0) {
              println("hi from ${coroutineContext[Job]} $channel $jobIndex: $jobIndex")
            }

            val x = channel.receive()

            if(jobIndex == 0) {
              println("woke up!")
            }

            x * x
        } to channel
      }

      val time = 50000
      println()
      println("awaiting $time ms...")
      delay(time)

      pairs.forEachIndexed { index, (job, channel) ->
        if(index == 0) {
          println("got $job, $channel")
        }

        channel.send(index)
        job.await()
        println(".")
      }
    }
  }
}


fun main(args: Array<String>) = runBlocking<Unit> {
  val jobs = List(100_000) {
    // launch a lot of coroutines and list their jobs
    launch {
      delay(40000L)
      print(".")
    }
  }

  jobs.forEach { it.join() } // wait for all jobs to complete
}