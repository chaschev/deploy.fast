package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.runtime.TaskContext
import mu.KLogging
import kotlin.reflect.KClass

typealias GradleTaskContext = ChildTaskContext<GradleExtension, GradleConfig>


class GradleExtension(
  config: (TaskContext<*, *, *>) -> GradleConfig = {GradleConfig()}
) : DeployFastExtension<GradleExtension, GradleConfig>("gradle", config
) {
  val zippedApp = ZippedAppExtension({ctx ->
    val gradleCtx =
    ctx.getParentCtx<GradleTaskContext>({it.task.name == "gradle"})!!

    ZippedAppConfig(
      "gradle",
      gradleCtx.config.version
    ).apply {
      archiveBaseUrl = "https://services.gradle.org/distributions"
//      archiveBasename = {"gradle-${ctx.config.version}-bin"}
      archiveName = {"gradle-${ctx.config.version}-bin.zip"}

      archiveChecksum = ChecksumHolder(sha256 = "fca5087dc8b50c64655c000989635664a73b11b9bd3703c7d6cabd31b7dcdb04")

      withSymlinks {
        "/usr/local/bin/gradle" to "bin/gradle"
      }
    }
  })


  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    GradleTasks(this@GradleExtension, parentCtx)
  }
}

class GradleConfig(
  var version: String = "4.7"
) : ExtensionConfig

class GradleTasks(ext: GradleExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<GradleExtension, GradleConfig>(ext, parentCtx) {
  suspend fun install() = extensionFun("install") {
    extension.zippedApp.tasks(this).install()
    ok
  }


  companion object : KLogging()
}


