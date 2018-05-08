package fast.api.ext

import fast.api.*
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.toFast
import fast.inventory.Host
import fast.lang.lazyVar
import fast.ssh.SshProvider
import fast.ssh.command.script.ScriptDsl.Companion.script
import fast.ssh.files.exists
import honey.lang.joinSpace
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
 * ok link
 * TODO keep releases
 * TODO rollback to release by ref/name
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
  val refId: String,
  /** a 6 digit ref id, i.e. 6 digits of commit hash in github */
  val shortRef: String,
  val log: String
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


class DepistranoGitCheckoutDSL {
  //user defined vars

  lateinit var url: String

  var target: GitCheckoutTarget? = null

  var folder: String by lazyVar { "${ctx.config.srcDir}/${ctx.config.projectName}" }

  //provided at runtime

  lateinit var ctx: DepistranoTaskContext
  val ssh: SshProvider by lazy { ctx.ssh }

  suspend fun checkout(): VCSUpdateResult {
    val config = ctx.config

    with(config) {
      val checkedOut = ssh.files().exists(folder)

      //TODO That's for target = null. Support custom checkout with target
      val r = script {
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

        capture("logCapture") {
          sh("git --no-pager log -3 --all --date-order --reverse")
        }
      }.execute(ssh)

      val refId = r["revisionCapture"]!!.text!!.toString()
      val log = r["logCapture"]!!.text!!.toString()

      logger.info { "checked out revision $refId" }

      return VCSUpdateResult(refId, refId.substring(0, 6), log)
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
  val ctx: DepistranoTaskContext,
  val configName: String = "default"
) : ExtensionConfig {
  lateinit var projectDir: String
  lateinit var projectName: String

  lateinit var checkoutMethod: CheckoutMethod

  var hostConfigs: List<DepistranoHost> = hosts.map { DepistranoHost(it) }

  var keepReleases: Int = 3

  var checkoutRef: String? = null

  val srcDir by lazy { "$projectDir/src" }
  val stashDir by lazy { "$projectDir/stash" }
  val releasesDir by lazy { "$projectDir/releases" }

  var build: (suspend (DepistranoTaskContext) -> ITaskResult<List<String>>)? = null

  /** checkout works with an empty folder */
  var checkout: DepistranoGitCheckoutDSL? = null
//  var update: ((ssh: SshProvider, folder: String, ref: String?) -> VCSUpdateResult)? = null

  var distribute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var execute: (suspend (DepistranoTaskContext) -> Unit)? = null

  var releaseTagDateFormat = DateTimeFormatterBuilder()
    .appendPattern("YYYYMMdd_HHmm_ss")
    .toFormatter()

  fun checkout(block: DepistranoGitCheckoutDSL.() -> Unit) {
    checkout = DepistranoGitCheckoutDSL().apply(block)
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

data class DepistranoGlobals(
  var release: String = ""
) {
  fun setVcsRef(r: VCSUpdateResult) {
    vcs = r

    val x = release

    if (!x.matches(DepistranoTasks.COMMIT_REF_REGEX)) {
      release = "${x}_${r.shortRef}"
    }
  }

  val artifacts = ArrayList<String>()

  lateinit var vcs: VCSUpdateResult
}


class DepistranoTasks(ext: DepistranoExtension, parentCtx: ChildTaskContext<*, *>)
  : NamedExtTasks<DepistranoExtension, DepistranoConfigDSL>(ext, parentCtx) {

  val prepareTask by extensionTask {
    script {
      mkdirs(
        config.projectDir,
        config.srcDir,
        config.stashDir,
        config.releasesDir
      )
    }.execute(ssh).toFast()

    //TODO refactor to use Task's context Type
    distribute ("depi.prepare") {
      val depiConfig = config

      app.addresses().take(1) with {

        logger.info { "I am ($address) building setting the depi globals, everyone is watching, path: $path" }

        val releaseName = LocalDateTime.now().format(depiConfig.releaseTagDateFormat)

        app.globalMap["${depiConfig.configName}.depi.globals"] = DepistranoGlobals(releaseName)

        logger.info { "depi.prepare $address - ${depiGlobals()}" }

        ok
      }
    }.await() as ITaskResult<List<String>?>
  }

  fun DepistranoTaskContext.depiGlobals() = app.globalMap["${config.configName}.depi.globals"] as DepistranoGlobals

  //checkout will set release tag
  val checkoutTask by extensionTask {
    config.apply {
      val r = with(checkout!!) {
        ctx = this@extensionTask
        checkout()
      }

      depiGlobals().setVcsRef(r)

      ssh.files().writeToString("$srcDir/vcsRef", "${r.shortRef} ${r.refId}")
      ssh.files().writeToString("$srcDir/vcsLog", r.log)
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
    distribute("depi.build") {
      app.addresses().take(1) with {
        logger.info { "I am ($address) building the project, everyone is watching, path: $path" }

        val r = config.build!!.invoke(this@extensionTask)

        val artifacts = r.value

        if(artifacts != null) depiGlobals().artifacts.addAll(artifacts)

        logger.info { "$address - finished building artifacts: ${r.value}" }

        r
      }
    }.await() as ITaskResult<List<String>?>
  }

  val distributeTask by extensionTask {
    val artifacts = buildTask.play(this).value

    var r: ITaskResult<*> = with(extension.stash.tasks(this)) {
      var r: ITaskResult<*> = ok

      r *= if (artifacts != null) {
        stash("depi.distribute", listOf(address), artifacts)
      } else {
        stash("depi.distribute")
      }

      r *= unstash("depi.distribute")

      r.asBoolean()
    }

    r *= script {
      mkdirs(releaseDir())
      sh("cp ${depiGlobals().artifacts.joinSpace()} ${releaseDir()}")
      sh("cp ${config.srcDir}/vcs* ${releaseDir()}")
      sh("echo ${depiGlobals().release} >${config.releasesDir}/current.txt")
    }.execute(ssh).toFast()

    r.asBoolean()
  }

  fun DepistranoTaskContext.releaseDir() = "${config.releasesDir}/${depiGlobals().release}"

  val linkTask by extensionTask {
    script {
      symlinks {
        "${config.releasesDir}/current" to releaseDir()
      }
    }.execute(ssh)
    //ok pre-create releases folder
    //ok stored shared depi object in a global map
    //ok create release folder via pattern
    //ok share artifact via global map
    //ok copy vcs files & artifacts to release folder
    //ok update current link, write current folder into a file
    ok
  }

  val executeTask by extensionTask {
    ok
  }

  val sweepTask by extensionTask {
    val releases = ssh.files()
      .ls(config.releasesDir)
      .filter { it.name[0].isDigit() }
      .sortedBy { it.name }

    val amountToCut = Math.max(releases.size - config.keepReleases, 0)

    val releasesToRemove = releases.take(amountToCut)

    logger.info { "found ${releases.size}, " +
      "removing $amountToCut (${releases.map { it.name }} " +
      "-> ${releases.takeLast(releases.size - amountToCut)})" }

    if(releasesToRemove.isNotEmpty())
      ssh.files().remove(*releasesToRemove.map { it.path }.toTypedArray(), recursive = true)

    ok
  }

  suspend fun installRequirements() = extensionFun("installRequirements") {
    extension.apt.tasks(extCtx).requirePackage("git")
  }

  suspend fun deploy() = extensionFun("installRequirements") {
    var r: ITaskResult<*>

    r = prepareTask.play(extCtx).abortIfError()
    r *= checkoutTask.play(extCtx).abortIfError()
//    r *= buildTask.play(this).abortIfError()
    r *= distributeTask.play(extCtx).abortIfError()
    r *= linkTask.play(extCtx).abortIfError()
    r *= executeTask.play(extCtx).abortIfError()
    r *= sweepTask.play(extCtx).abortIfError()

    r.asBoolean()
  }

  suspend fun updateFileTask() = extensionFun("updateFileTask") {
    ok
  }

  suspend fun updateFile() = updateFileTask()

  companion object : KLogging() {
    val COMMIT_REF_REGEX = """^.*_[0-9a-f]{6}$""".toRegex()
  }
}





