package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.lang.lazyVar
import fast.ssh.SshProvider
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.files.exists
import mu.KLogging
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder

typealias DepistranoTaskContext = ChildTaskContext<DepistranoExtension, DepistranoConfigDSL>

/**
 * ok simple checkout
 *  checkout branch
 *  checkout tag
 * TODO checkout ref
 * ok test 3 machines
 * TODO keep releases
 * TODO rollback to release
 * TODO take a break
 * TODO FUCK IT
 */
class DepistranoExtension(
  config: (DepistranoTaskContext) -> DepistranoConfigDSL
) : DeployFastExtension<DepistranoExtension, DepistranoConfigDSL>(
  "depistrano", config
) {
  val apt = AptExtension()
  val stash = StashExtension({ parentCtx ->
    val ctx = parentCtx.getParentCtx<DepistranoTaskContext> { it.task.name == "depistrano" }!!

    StashConfig(
      ctx.config.hosts,
      ctx.config.stashDir
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

class DepistranoGitCheckoutDSL {
  //user defined vars

  lateinit var url: String

  var target: GitCheckoutTarget? = null

  var folder: String by lazyVar {"${ctx.config.srcDir}/${ctx.config.projectName}"}

  //provided at runtime

  lateinit var ctx: DepistranoTaskContext
  val ssh: SshProvider by lazy {ctx.ssh}

  suspend fun checkout(): VCSUpdateResult {
    val config = ctx.config

    with(config) {
      val checkedOut = ssh.files().exists(folder)

      //TODO That's for target = null. Support custom checkout with target
      val refId = script {
        cd(srcDir)

        capture {
          handleConsoleInput { console, newText ->
            if (newText.contains("Password for ")) {
              val password = ctx.getStringVar("git.password")
              console.writeln(password)
            }
          }

          if (!checkedOut) {
            sh("git clone $url")
            cd(folder)
          } else {
            cd(folder)

            sh("git pull")
          }
        }

        capture("revisionCapture") {
          sh("git rev-parse --verify HEAD")
        }
      }.execute(ssh)["revisionCapture"]!!.text!!.toString()

      logger.info { "checked out revision $refId" }

      return VCSUpdateResult(refId, refId.substring(0, 6))
    }
  }

  data class GitCheckoutTarget(
    val ref: String? = null,
    val tag: String? = null,
    val branch: String? = null
  )

  companion object : KLogging()
}


class DepistranoConfigDSL(
  val hosts: List<Host>,
  val ctx: DepistranoTaskContext
) : ExtensionConfig {
  lateinit var projectDir: String
  lateinit var projectName: String

  lateinit var checkoutMethod: CheckoutMethod

  var hostConfigs: List<DepistranoHost> = hosts.map { DepistranoHost(it) }

  var keepReleases: Int = 3

  var checkoutRef: String? = null

  val srcDir by lazy { "$projectDir/src" }
  val stashDir by lazy { "$projectDir/stash" }

  var build: (suspend (DepistranoTaskContext) -> ITaskResult<List<String>>)? = null

  /** checkout works with an empty folder */
  var checkout: (suspend (ctx: DepistranoTaskContext, ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null
  var checkout3: DepistranoGitCheckoutDSL? = null
//  var update: ((ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null

  var distribute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var execute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var releaseTagDateFormat = DateTimeFormatterBuilder()
    .appendPattern("YYYYMMdd_HHmm_ss")
    .toFormatter()

  val runtime = DepistranoRuntime(this)

  fun checkout3(block: DepistranoGitCheckoutDSL.() -> Unit) {
    checkout3 = DepistranoGitCheckoutDSL().apply(block)
  }

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


  companion object {
    fun depistranoDsl(ctx: DepistranoTaskContext, block: DepistranoConfigDSL.() -> Unit): DepistranoConfigDSL {
      val dsl = DepistranoConfigDSL(ctx.app.hosts, ctx)

      dsl.apply(block)

      return dsl
    }

    fun depistrano(block: DepistranoConfigDSL.() -> Unit) = DepistranoExtension(
      { depistranoDsl(it, block) }
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
    config.apply {
      val r = with(checkout3!!) {
        ctx = this@extensionTask
        checkout()
      }

      runtime.releaseName += "_ref${r.ref}"

      ssh.files().writeToString("$srcDir/vcsRef", "${r.ref} ${r.refId}")
    }
    ok
  }

  /**
   * Build task will return null if the party is not building the project.
   * Distribute task
   *
   * Delete this ?!
   *
   * Architectural note: handling multiple jobs:
   *
   * val job1 = runOnce({job1})
   * val job2 = runOnce({job2})
   *
   * job1.await()
   * job2.await()
   */
  val buildTask by extensionTask {
    val depiConfig = config

    distribute("depi.build") {
      app.addresses().take(1) with {
        logger.info { "I am ($address) building the project, everyone is watching, path: $path" }

        val r = depiConfig.build!!.invoke(this@extensionTask)

        logger.info { "$address - finished building artifacts: ${r.valueNullable()}" }

        r
      }
    }.await() as ITaskResult<List<String>?>
  }

  val distributeTask by extensionTask {
    val artifacts = buildTask.play(this).value

    with(extension.stash.tasks(this)) {
      var r: ITaskResult<*> = ok

      r *= if(artifacts != null) {
        stash("depi.distribute", listOf(address), artifacts)
      } else {
        stash("depi.distribute")
      }

      r *= unstash("depi.distribute")

      r.asBoolean()
    }
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





