package fast.api.ext

import fast.api.*
import fast.dsl.ServiceStatus
import fast.dsl.TaskResult
import fast.inventory.Host
import fast.ssh.command.script.ScriptDsl.Companion.processScript
import fast.ssh.command.script.ScriptDsl.Companion.script
import mu.KLogging

typealias CassandraTaskContext = ChildTaskContext<CassandraExtension, CassandraConfig>


class CassandraExtension(
  config: (CassandraTaskContext) -> CassandraConfig
) :
  DeployFastExtension<CassandraExtension, CassandraConfig>(
    "cassandra", config
  ) {

  val apt = AptExtension({  AptConfig() })

  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    CassandraTasks(this@CassandraExtension, parentCtx)
  }
}

data class CassandraInstallationResult(
  val status: ServiceStatus,
  val timeMs: Long
)

typealias CIR = CassandraInstallationResult

class CassandraTasks(ext: CassandraExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<CassandraExtension, CassandraConfig>(ext, parentCtx) {
  suspend fun install(): ITaskResult<Boolean> {
    return LambdaTask("install", extension) {

      val cassandra = User("cassandra")
      val appPath = "/var/lib/cassandra"
      val appBin = "$appPath/bin"
      val tmpDir = "/tmp/cassandra"
      val extractedTmpHomePath = "$tmpDir/${config.distroName}"

      script {
        settings {
          abortOnError = true
        }

        mkdir(tmpDir)

        cd(tmpDir)

        wget(
          config.archiveUrl,
          config.sha1
        )

        untar(
          file = "/tmp/cassandra/${config.archiveName}"
        )

        addUser(cassandra)

        rights(
          paths = listOf(
            "/var/lib/cassandra",
            "/var/log/cassandra"
          ),
          rights = Rights.userOnlyReadWriteFolder.copy(owner = cassandra),
          create = true,
          recursive = false
        ) { sudo = true; abortOnError = true }

        sh("cp -R $extractedTmpHomePath/* /var/lib/cassandra") { withUser = cassandra.name; abortOnError = false }

        rights(
          paths = listOf(
            "$appBin/nodetool",
            "$appBin/cassandra",
            "$appBin/cqlsh"
          ),
          rights = Rights.executableAll
        ) {
          withUser = "cassandra"
          abortOnError = true
        }

        //todo: try running cassandra as a newly created user ok - works from CD or with magic
        //todo: fix yaml download
        //todo: configure
        //todo: install service
        //todo: FUCK IT

        symlinks {
          "$appBin/cassandra" to "/usr/local/bin/cassandra"
          "$appBin/nodetool" to "/usr/local/bin/nodetool"
          "$appBin/cqlsh" to "/usr/local/bin/cqlsh"

          sudo = true
        }

        CIR(ServiceStatus.installed, 0)
      }.execute(ssh)

      val cassandraYamlPath = "/var/lib/cassandra/conf/cassandra.yaml"

      val confString: String = ssh.files().readAsString("$extractedTmpHomePath/conf/cassandra.yaml")

      /* TODO: EDIT THE DEFAULT CONF */

      ssh.files().writeToString(cassandraYamlPath, confString)

      /*
       todo change this to apt call to service installation, which not yet exists
       todo exit with service.state(installed, running)
         service.install
         service.ensureState(installed, running)
         service.ensureConfiguration()
      */
//      extension.apt.tasks(this@LambdaTask).install()



      TaskResult.ok
    }.play(extCtx)
  }

  companion object : KLogging()
}

class CassandraConfig(
  val hosts: List<Host> = ArrayList(),
  val hostConfigs: List<CassandraHost> = hosts.map { CassandraHost(it) }

  ) : ExtensionConfig {
  var version = "3.11.2"
  val distroName = "apache-cassandra-$version"
  var archiveName = "apache-cassandra-$version-bin.tar.gz"
  var archiveUrl = "http://mirror.bit.edu.cn/apache/cassandra/$version/$archiveName"
  var sha1 = "f52a65ffddeff553f2ac3f8f3fd3dd74317fe793"

}

data class CassandraHost(
  val host: Host,
  val memory: Int = 1024
)


/*
class CassandraExtension(): DeployFastExtension() {
  val zippedApp = ZippedAppExtension()

  override val tasks: NamedExtTasks
    get() = TODO("not implemented")

  companion object {
    fun dsl() = DeployFastDSL.deployFast(CassandraExtension()) {
      //      autoInstall()

      info {
        name = "Cassandra Extension"
        author = "Andrey Chaschev"
      }

      beforePlay {
        init {
          ext.zippedApp.configure("cassandra", "3.1.12", "TODO") {
            archiveName = "$name-$version.tar.gz"

            symlinks {
              "cassandra" to "/bin/cassandra"
            }
          }

          ext.zippedApp.tasks.install().after.append {
            // TODO install as a service
            // TODO run
            TaskResult()
          }
        }
        play {
          task("update_conf") {
            //TODO(process template)
            TaskResult()
          }

          task("install") {
            ext.zippedApp.tasks.install().run()
          }

          task("install service") {
            //            ext.zippedApp.tasks.installService().run()
            TODO()
          }
        }

        afterPlay {
          task("check_install") {
            //            ext.zippedApp.tasks.getServiceState(installed = true, running = true).run()
            TODO()
          }
        }
      }



      play {
        task("install_cassandra") {
          ext.zippedApp.tasks.install().run()
        }

//      task("create_symlinks"){
//        ext.zippedApp
//      }
      }

    }

  }
}
*/


