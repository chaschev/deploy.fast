package fast.dsl.ext

import fast.dsl.*
import fast.runtime.TaskContext
import fast.ssh.command.JavaVersion
import fast.ssh.command.JavaVersionCommand
import fast.ssh.command.JavaVersionCommand.Companion.javaVersion
import fast.ssh.command.Version
import fast.ssh.runAndWait
import fast.ssh.tryFind
import mu.KLogging

data class OpenJdkConfig(
  var version: Int = 8,
  var pack: String = "openjdk-$version-jdk"
) : ExtensionConfig

class OpenJDKTasks(val ext: OpenJdkExtension, taskCtx: TaskContext) : NamedExtTasks(
  ext as DeployFastExtension<ExtensionConfig>, taskCtx
) {
  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process

  override suspend fun getStatus(): ServiceStatus {
    return (ExtensionTask("getStatus", extension = extension) {
      val installed = ext.apt.tasks(this).listInstalled("openjdk")

      println("installed jdk packages: $installed")

      if (installed.isEmpty())
        ServiceStatus.notInstalled
      else
        ServiceStatus.installed
    }.play(taskCtx) as TaskValueResult<ServiceStatus>).value
  }

  suspend fun uninstall(): Boolean {
    return (ExtensionTask("getStatus", extension = extension) {

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
        val installed = apt.tasks(this).dpkgListInstalled("openjdk").value

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

      return@ExtensionTask TaskValueResult(installed1.isEmpty() && installed2.isEmpty())
    }.play(taskCtx) as TaskValueResult<Boolean>).value
  }

  suspend fun javaVersion(): JavaVersion? =
    (ExtensionTask("javacVersion", extension) {
      ssh.runAndWait("java -version",
        process = { console ->
          val version = JavaVersion.parseJavaVersion(console.stdout.toString())

          version
        }).toFast()
    }.play(taskCtx) as TaskValueResult<JavaVersion?>).value


  suspend fun javacVersion(): Int? =
    (ExtensionTask("javacVersion", extension) {
      ssh.runAndWait("javac -version",
        process = { console ->
          val version = "(\\d+)".toRegex().tryFind(console.stdout.toString())?.get(1)?.toInt()

          version
        }).toFast()
    }.play(taskCtx) as TaskValueResult<Int?>).value

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
    requiredJavaVersion: Version = Version.parse("9.9"),
    options: JavaInstallOptions = JavaInstallOptions.DEFAULT
  ) =

    (ExtensionTask("javacVersion", extension) {
      val apt = ext.apt

      val javaVersion = javaVersion()
      val javacVersion = javacVersion()

      logger.info { "found java $javaVersion, javac $javacVersion" }

      if (options.force
        || javaVersion ?: Version.ZERO < requiredJavaVersion
        || javacVersion ?: 0 < requiredJavaVersion[0]) {

        if (options.force)
          logger.info { "java installation is forced" }
        else
          logger.info { "java is not installed or less than $requiredJavaVersion" }

        if (options.clearPreviousVersions) {
          if (!uninstall()) {
            throw IllegalStateException("could not delete old jdk")
          }
        }

        logger.info { "installing java $requiredJavaVersion" }

        val r = apt.tasks(this).install(
          pack = (config as OpenJdkConfig).pack,
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
    })


  companion object : KLogging() {

  }
}

/**
 * This extension will generate vagrant project file.
 */
class OpenJdkExtension(
  config: (TaskContext) -> OpenJdkConfig
) : DeployFastExtension<OpenJdkConfig>(
  "openjdk", config
) {
  val apt = AptExtension({ AptExtensionConfig() })

  override val tasks = { ctx: TaskContext -> OpenJDKTasks(this, ctx) }
}


