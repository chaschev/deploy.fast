package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.runtime.TaskContext
import mu.KLogging

typealias GradleTaskContext = ChildTaskContext<GradleExtension, GradleConfig>


class GradleExtension(
  config: (TaskContext<*, *, *>) -> GradleConfig
) : DeployFastExtension<GradleExtension, GradleConfig>("gradle", config
) {
  val zippedApp = ZippedAppExtension({parentCtx ->
    val ctx = parentCtx as GradleTaskContext

    ZippedAppConfig(
      "gradle",
      ctx.config.version
    ).apply {
      archiveBaseUrl = "https://services.gradle.org/distributions"
      archiveBasename = {"gradle-${ctx.config.version}-bin"}
      archiveName = {"gradle-${ctx.config.version}-bin.zip"}

      archiveChecksum = ChecksumHolder(sha256 = "203f4537da8b8075e38c036a6d14cb71b1149de5bf0a8f6db32ac2833a1d1294")

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


