package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.inventory.Host
import fast.ssh.SshProvider
import mu.KLogging
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

typealias DepistranoTaskContext = ChildTaskContext<DepistranoExtension, DepistranoConfigDSL>

/**
 * This extension will generate depistrano project file.
 */
class DepistranoExtension(
  config: (DepistranoTaskContext) -> DepistranoConfigDSL
) : DeployFastExtension<DepistranoExtension, DepistranoConfigDSL>(
  "depistrano", config
) {
  val apt = AptExtension()

  override val tasks = { parentCtx: ChildTaskContext<*, *> ->
    DepistranoTasks(this@DepistranoExtension, parentCtx)
  }
}

data class VCSUpdateResult(
  /** a 6 digit ref id, i.e. 6 digits of commit hash in github */
  val ref: String,
  val refId: String
)

interface ICheckoutMethod {
  val ssh: SshProvider
  val folder: String

  val checkout: ICheckoutMethod.() -> VCSUpdateResult
  val update: ICheckoutMethod.() -> VCSUpdateResult
}

data class GitCheckoutConfig(
  val repoUrl: String,
  val user: String,
  val password: String?,
  val branch: String
)

data class CheckoutMethod(
  override val ssh: SshProvider,
  override val folder: String,
  override val checkout: ICheckoutMethod.() -> VCSUpdateResult,
  override val update: ICheckoutMethod.() -> VCSUpdateResult
) : ICheckoutMethod

class DepistranoRuntime(
 val dsl: DepistranoConfigDSL
) {
  var releaseName: String = dsl.releaseTagDateFormat.format(Instant.now())

}

class DepistranoConfigDSL(
  val hosts: List<Host>,
  val ctx: DepistranoTaskContext
) : ExtensionConfig {
  lateinit var projectDir: String
  lateinit var checkoutMethod: CheckoutMethod

  var hostConfigs: List<DepistranoHost> = hosts.map { DepistranoHost(it) }

  var keepReleases: Int = 3

  var checkoutRef: String? = null

  var build: (suspend (DepistranoTaskContext) -> List<String>)? = null

  /** checkout works with an empty folder */
  var checkout: (suspend (ctx: DepistranoTaskContext, ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null
//  var update: ((ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null

  var distribute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var execute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var releaseTagDateFormat = DateTimeFormatterBuilder()
    .appendPattern("yyyyMMdd_HHmm_ss")
    .toFormatter()

  val runtime = DepistranoRuntime(this)

  fun checkout(block: suspend (ctx: DepistranoTaskContext, ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult) {
    checkout = block
  }

  fun build(block: suspend DepistranoTaskContext.() -> List<String>) {
    build = block
  }

  fun distribute(block: (suspend (DepistranoTaskContext) -> Unit)? = null) {
    distribute = block
  }

  fun execute(block: suspend DepistranoTaskContext.() -> Unit) {
    execute = block
  }

  val srcDir by lazy { "$projectDir/src" }

  companion object {
    fun depistranoDsl (ctx: DepistranoTaskContext, block: DepistranoConfigDSL.() -> Unit): DepistranoConfigDSL {
      val dsl = DepistranoConfigDSL(ctx.app.hosts, ctx)

      dsl.apply(block)

      return dsl
    }

    fun depistrano(block: DepistranoConfigDSL.() -> Unit) = DepistranoExtension(
      {depistranoDsl(it, block)}
    )
  }
}


/**
 * TODO:
 *  Extensions
 *   apt-backed tool => returns version, installs, checks status
 *   stash => distributing a file across the cluster
 */
data class DepistranoHost(
  val host: Host,
  val hostname: String = host.name
) {

}


class DepistranoTasks(ext: DepistranoExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<DepistranoExtension, DepistranoConfigDSL>(ext, parentCtx) {

  val prepareTask by extensionTask {
    ssh.files().mkdirs(config.projectDir)

    ok
  }

  //checkout will set release tag
  val checkoutTask by extensionTask {
    with(config) {
      val r = checkout!!.invoke(
        this@extensionTask,
        ssh,
        srcDir,
        checkoutRef
      )

      runtime.releaseName += "_ref${r.ref}"

      ssh.files().writeToString("$srcDir/vcsRef", "${r.ref} ${r.refId}")
    }
    ok
  }

  val buildTask by extensionTask {
    config.build!!.invoke(this@extensionTask)
    ok
  }

  val distributeTask by extensionTask {
    TODO()
    ok
  }

  val linkTask by extensionTask {

    ok
  }

  val executeTask by extensionTask {
    ok
  }

  val sweepTask by extensionTask {
    ok
  }

  suspend fun installRequirements() = extensionFun("installRequirements") {
    extension.apt.tasks(this).requirePackage("git")
  }

  suspend fun updateFileTask() = extensionFun("updateFileTask") {
    ok
  }

  suspend fun updateFile() = updateFileTask()

  companion object : KLogging()
}





