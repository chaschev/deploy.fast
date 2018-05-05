package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.ssh.SshProvider
import fast.ssh.command.script.ScriptDsl.Companion.script
import mu.KLogging
import java.time.LocalDateTime
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
  val stash = StashExtension({ parentCtx ->
    val ctx = parentCtx as DepistranoTaskContext
    StashConfig(
      ctx.config.hosts,
      stashFolder = ctx.config.projectDir
    )
  })

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
  var releaseName: String = dsl.releaseTagDateFormat.format(LocalDateTime.now())

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

  var build: (suspend (DepistranoTaskContext) -> ITaskResult<List<String>>)? = null

  /** checkout works with an empty folder */
  var checkout: (suspend (ctx: DepistranoTaskContext, ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null
//  var update: ((ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null

  var distribute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var execute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var releaseTagDateFormat = DateTimeFormatterBuilder()
    .appendPattern("YYYYMMdd_HHmm_ss")
    .toFormatter()

  val runtime = DepistranoRuntime(this)

  fun checkout(block: suspend (ctx: DepistranoTaskContext, ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult) {
    checkout = block
  }

  fun build(block: suspend DepistranoTaskContext.() -> ITaskResult<List<String>>) {
    build = block
  }

  fun distribute(block: (suspend (DepistranoTaskContext) -> Unit)? = null) {
    distribute = block
  }

  fun execute(block: suspend DepistranoTaskContext.() -> Unit) {
    execute = block
  }

  val srcDir by lazy { "$projectDir/src" }
  val stashDir by lazy { "$projectDir/stash" }

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
    script {
      mkdirs(
        config.projectDir,
        config.srcDir,
        config.stashDir
      )
    }.execute(ssh).toFast()
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

  /**
   * Architectural note: handling multiple jobs:
   *
   * val job1 = runOnce({job1})
   * val job2 = runOnce({job2})
   *
   * job1.await()
   * job2.await()
   */
  val buildTask by extensionTask {
    val artifacts = app.runOnce("depistrano_build_${session.path}") {
      app.globalMap["depistrano_build_owner_${session.path}"] = address

      logger.info { "I am ($address) building the project, everyone is waiting" }

       config.build!!.invoke(this@extensionTask).mapValue { address to it }

    }.await()

    artifacts
  }

  val distributeTask by extensionTask {
    val (owner, artifacts) = buildTask.play(this).value

    val tasks = extension.stash.tasks(this)

    var r: ITaskResult<*> = tasks.stash(listOf(owner), artifacts)

    r *= tasks.unstash()

    r.asBoolean()
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

  suspend fun deploy() = extensionFun("installRequirements") {
    var r: ITaskResult<*>

    r = prepareTask.play(this).abortIfError()
    r *= checkoutTask.play(this).abortIfError()
//    r *= buildTask.play(this).abortIfError()
    r *= distributeTask.play(this).abortIfError()
    r *= linkTask.play(this).abortIfError()
    r *= executeTask.play(this).abortIfError()
    r *= sweepTask.play(this).abortIfError()

    r.asBoolean()
  }

  suspend fun updateFileTask() = extensionFun("updateFileTask") {
    ok
  }

  suspend fun updateFile() = updateFileTask()

  companion object : KLogging()
}





