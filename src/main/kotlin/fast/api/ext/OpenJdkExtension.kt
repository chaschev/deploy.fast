package fast.api.ext

import fast.api.*
import fast.dsl.*
import fast.ssh.command.JavaVersion
import fast.ssh.command.Version
import fast.ssh.runAndWaitProcess
import fast.log.KLogging

class OpenJdkExtension(
  config: (OpenJDKTaskContext) -> OpenJdkConfig
) : DeployFastExtension<OpenJdkExtension, OpenJdkConfig>(
  "openjdk", config
) {
  val apt = AptExtension({ AptConfig() })

  override val tasks = { ctx: ChildTaskContext<*, *> -> OpenJDKTasks(this, ctx) }
}


data class OpenJdkConfig(
  var version: Int = 8,
  var pack: String = "openjdk-$version-jdk"
) : ExtensionConfig

typealias OpenJDKTask<R> = ExtensionTask<R, OpenJdkExtension, OpenJdkConfig>
typealias OpenJDKTaskContext = ChildTaskContext<OpenJdkExtension, OpenJdkConfig>

class OpenJDKTasks(val ext: OpenJdkExtension, parentCtx: ChildTaskContext<*, *>) :
  NamedExtTasks<OpenJdkExtension, OpenJdkConfig>(
    ext, parentCtx
  ) {

  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process
  override suspend fun getStatus(): ITaskResult<ServiceStatus> {
    val result = OpenJDKTask("getStatus", extension) {
      val installed = ext.apt.tasks(this).listInstalled("openjdk")

      println("installed jdk packages: $installed")

      if (installed.isEmpty())
        TaskResult(value = ServiceStatus.notInstalled, ok = false)
      else
        TaskResult(value = ServiceStatus.installed, ok = true)
    }.play(extCtx)

    return result
  }

  suspend fun uninstall() = extensionFun("uninstall") {

    val apt = ext.apt

    logger.info { "trying to delete old installed openjdk packages..." }

    for (i in 1..3) {
      val installed = apt.tasks(this).listInstalled("openjdk")

      if (installed.isEmpty()) {
        logger.info { "found 0 packages installed, finishing" }
      }

      logger.info { "#$i. found ${installed.size} old packages: ${installed.joinToString(",")}" }

      for (pack in installed) {
        logger.info { "removing $pack..." }
        apt.tasks(this).remove(pack)
      }
    }

    logger.info { "dpkg: trying to delete old openjdk packages..." }

    for (i in 1..3) {
      val installed = apt.tasks(this).dpkgListInstalled("openjdk").value!!

      if (installed.isEmpty()) {
        logger.info { "found 0 packages installed, finishing" }
      }

      logger.info { "#$i. found ${installed.size} old packages: ${installed.joinToString(",")}" }

      for (pack in installed) {
        logger.info { "removing $pack..." }
        apt.tasks(this).dpkgRemove(pack.name)
      }
    }

    val installed1 = apt.tasks(this).listInstalled("openjdk")
    val installed2 = apt.tasks(this).dpkgListInstalled("openjdk").value

    TaskResult(installed1.isEmpty() && installed2?.isEmpty() != false)
  }

  suspend fun javaVersion(): JavaVersion? =
    (ExtensionTask("javacVersion", extension) {
      ssh.runAndWaitProcess("java -version",
        process = { console ->
          val version = JavaVersion.parseJavaVersion(console.stdout.toString())

          version
        }).toFast()
    }.play(extCtx)).value


  suspend fun javacVersion(): JavaVersion? =
    (OpenJDKTask("javacVersion", extension) {
      ssh.runAndWaitProcess("javac -version",
        process = { console ->
          println("parsing java version!")

          val version = JavaVersion.parseJavacVersion(console.stdout.toString())

          println("done parsing java version!")

          version
        }).toFast()
    }.play(extCtx)).value

  data class JavaInstallOptions(
    val force: Boolean = false,
    val clearPreviousVersions: Boolean = true,
    val forceRemoveOldPackages: Boolean = true
  ) {
    companion object {
      val DEFAULT = JavaInstallOptions()
    }
  }

  suspend fun installJava(
    options: JavaInstallOptions = JavaInstallOptions.DEFAULT
  ) = extensionFun("installJava") {

    val apt = ext.apt

    val javaVersion = javaVersion()
    val javacVersion = javacVersion()

    val conf = config

    val requiredJavaVersion: Version = Version.parse(conf.version.toString())

    logger.info { "found java $javaVersion, javac $javacVersion" }

    val result = if (options.force
      || javaVersion ?: Version.ZERO < requiredJavaVersion
      || javacVersion ?: Version.ZERO < requiredJavaVersion) {

      if (options.force)
        logger.info { "java installation is forced" }
      else
        logger.info { "java is not installed or less than $requiredJavaVersion" }

      if (options.clearPreviousVersions) {
        if (!uninstall().ok) {
          throw IllegalStateException("could not delete old jdk")
        }
      }

      logger.info { "installing java ${requiredJavaVersion.asString()}" }

      apt.tasks(this).update()

      val r = apt.tasks(this).install(
        pack = config.pack,
        options = AptTasks.InstallOptions(true)
      )

      if (r.ok) {
        logger.info { "installed java $requiredJavaVersion" }
        r
      } else {
        logger.info { "could not install java $requiredJavaVersion: $r" }
        throw Exception("could not install java $requiredJavaVersion")
      }
    } else {
      logger.info { "there is no need for java update, sir" }
      TaskResult.ok
    }

    result.asBoolean()
  }


  companion object : KLogging() {

  }
}


