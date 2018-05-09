package fast.api.ext

import fast.api.*
import fast.dsl.ServiceStatus
import fast.dsl.TaskResult
import fast.inventory.Host
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.log.KLogging

typealias CassandraTaskContext = ChildTaskContext<CassandraExtension, CassandraConfig>

class CassandraExtension(
  config: (CassandraTaskContext) -> CassandraConfig
) :
  DeployFastExtension<CassandraExtension, CassandraConfig>(
    "cassandra", config
  ) {

  val apt = AptExtension()
  val cassandraService = SystemdExtension({
    SystemdConfig(
      name = "cassandra",
      exec = "/var/lib/cassandra/bin/cassandra -p /var/lib/cassandra/cassandra.pid",
      directory = "/var/lib/cassandra",
      user = "cassandra",
      pidfile = "/var/lib/cassandra/cassandra.pid"
    )
  })

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
  suspend fun install() = extensionFun("install") {
    val cassandra = User("cassandra")
    val appPath = "/var/lib/cassandra"
    val appBin = "$appPath/bin"
    val tmpDir = "/tmp/cassandra"
    val extractedTmpHomePath = "$tmpDir/${config.distroName}"
    val cassandraYamlPath = "$appPath/conf/cassandra.yaml"
    val cassandraTmpYamlPath = "$tmpDir/cassandra.conf"

    extension.apt.tasks(this).requirePackage("python")

    script {
      settings {
        abortOnError = true
      }

      mkdirs(tmpDir)

      cd(tmpDir)

      wget(
        config.archiveUrl,
        Checksum(sha1 = config.sha1)
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

      sh("cp -f $cassandraYamlPath $cassandraTmpYamlPath") { sudo = true }

      rights(
        path = cassandraTmpYamlPath,
        rights = Rights.writeAll
      ) { sudo = true }

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

      //ok: try running cassandra as a newly created user ok - works from CD or with magic
      //ok: fix yaml download
      //ok: configure
      //ok: install python
      //ok: check cqlsh
      //ok: install service, run from cd
      //ok: FUCK IT
      //ok: take a break

      symlinks {
        "/usr/local/bin/cassandra" to "$appBin/cassandra"
        "/usr/local/bin/nodetool" to "$appBin/nodetool"
        "/usr/local/bin/cqlsh" to "$appBin/cqlsh"

        sudo = true
      }

      CIR(ServiceStatus.installed, 0)
    }.execute(ssh)


    val confString: String = ssh.files().readAsString("$extractedTmpHomePath/conf/cassandra.yaml")

    val host = config.hosts.find { it.listenAddress == ssh.address }
      ?: throw Exception("host not found in cassandra conf: ${ssh.address}")

    val patcher = SlyYamlPatcher(StringBuilder(confString))
      .replaceField("cluster_name", config.cluster.name)
      .replaceField("- seeds", '"' + config.cluster.seedIps.joinToString(",") + '"')
      .replaceField("listen_address", host.listenAddress)
      .replaceField("rpc_address", host.rpcAddress)
      .replaceField("start_rpc", host.startRpc.toString())

    ssh.files().remove(cassandraTmpYamlPath)

    ssh.files().writeToString(cassandraTmpYamlPath, patcher.yaml.toString())

    script {
      sh("cp -f $cassandraTmpYamlPath $cassandraYamlPath") { sudo = true }

      //TODO: extract into "edit with rights" pattern
      rights(
        path = cassandraTmpYamlPath,
        rights = Rights.userReadWrite.copy(owner = cassandra)
      ) { sudo = true }
    }.execute(ssh)

    with(extension.cassandraService.tasks(this)) {
      installService()
      startAndAwait()
    }

    /*
     todo change this to apt call to service installation, which not yet exists
     todo exit with service.state(installed, running)
     ok install python for cqlsh
       service.install
       service.ensureState(installed, running)
       service.ensureConfiguration()
    */
//      extension.apt.tasks(this@LambdaTask).install()


    TaskResult.ok
  }


  companion object : KLogging()
}

class CassandraConfig(
  clusterName: String,
  _hosts: List<Host> = ArrayList(),
  val hosts: List<CassandraHost> = _hosts.map { CassandraHost(it) }
) : ExtensionConfig {
  var version = "3.11.2"
  val distroName = "apache-cassandra-$version"
  var archiveName = "apache-cassandra-$version-bin.tar.gz"
  var archiveUrl = "http://mirror.bit.edu.cn/apache/cassandra/$version/$archiveName"
  var sha1 = "f52a65ffddeff553f2ac3f8f3fd3dd74317fe793"

  var cluster = CassandraClusterConfig(clusterName, _hosts.take(3).map { it.address })
}


data class CassandraClusterConfig(
  val name: String = "Test Cluster",
  val seedIps: List<String>
)

data class CassandraHost(
  val host: Host,
  val listenAddress: String = host.address,
  val startRpc: Boolean = true,
  val rpcAddress: String = host.address,
  val memory: Int = 1024
)

class SlyYamlPatcher(val yaml: StringBuilder) {
  fun replaceField(field: String, value: String): SlyYamlPatcher {
    val startOfFieldIndex = yaml.indexOf("$field: ")
    val startOfValue = startOfFieldIndex + field.length + ": ".length
    val endOfValueIndex = yaml.indexOf('\n', startOfFieldIndex)

    yaml.replace(startOfValue, endOfValueIndex, value)

    return this
  }
}





