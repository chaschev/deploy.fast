package fast.dsl

import fast.api.*
import fast.ssh.command.CommandResult


data class AggregatedValue(
  val list: ArrayList<Any> = ArrayList<Any>()
) {
  constructor(vararg items: Any) : this(items.toCollection(ArrayList()))
}


typealias AnyAnyResult = ITaskResult<*>
typealias AnyResult = ITaskResult<*>
typealias BooleanResult = ITaskResult<Boolean>


data class TaskResult<R>(
  override val value: R,
  override val ok: Boolean = defaultIsOk(value),
  override val modified: Boolean = false,
  val stdout: String = "",
  val stderr: String? = null,
  val code: Int = 0,
  val comment: String? = null
) : ITaskResult<R> {
  override fun text(): String {
    return stdout.trim()
  }

  companion object {
    val ok: ITaskResult<Boolean> = TaskResult(value = true)
    fun failed(comment: String): ITaskResult<Boolean> = TaskResult(value = false, stdout = comment)

    fun <R> defaultIsOk(value : R) =
      when (value) {
        is Boolean -> value
        is Int -> value == 0
        else -> true
      }
  }
}

class CommandLineResult<T>(
  override val ok: Boolean,
  override val modified: Boolean,
  override val value: T,
  val commandResult: CommandResult<T>
) : ITaskResult<T> {
  override fun toString(): String {
    return """Result(ok=$ok, modified=$modified, value=$value)"""
  }

  override fun text() = commandResult.console.stdout.trim().toString()
}

fun <T> CommandResult<T>.toFast(modified: Boolean = false): CommandLineResult<T> {
  return CommandLineResult(
    this.console.result.isOk(), modified, value, this
  )
}

typealias AnyTask = Task<Any, *, ExtensionConfig>
typealias AnyTaskExt<EXT> = Task<Any, EXT, ExtensionConfig>

enum class RunningStatus {
  notStarted, started, running, stopped, aborted, unknown, notApplicable
}

enum class InstalledStatus {
  installed, notInstalled, installedWrongVersion, uninstalled, unknown
}

data class ServiceStatus(
  val installation: InstalledStatus,
  val running: RunningStatus = RunningStatus.notApplicable,
  val pid: Int? = null
) {
  companion object {
    val installed = ServiceStatus(InstalledStatus.installed)
    val notInstalled = ServiceStatus(InstalledStatus.notInstalled)
  }
}

class InfoDSL(
) {
  var name: String = ""
  var author: String = ""
  var description: String = ""
}

inline fun <T> Any?.ifNotNull(block: () -> T): T {
  return block.invoke()
}


class DeployFastAppDSL<APP : DeployFastApp<APP>>(ext: APP)
  : DeployFastDSL<NoConfig, APP>(ext) {

}

