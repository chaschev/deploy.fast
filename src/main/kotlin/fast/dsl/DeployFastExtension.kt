package fast.dsl

import fast.runtime.AllSessionsRuntimeContext
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf

abstract class DeployFastExtension {
  /* Named extension tasks */
  open val tasks: NamedExtTasks = NamedExtTasks()

  lateinit var global: AllSessionsRuntimeContext

  internal fun init(allContext: AllSessionsRuntimeContext) {
    global = allContext

    for (ext in extensions) {
      ext.init(allContext)
    }
  }

  val extensions: List<DeployFastExtension> by lazy {
    val properties = this::class.declaredMemberProperties

    properties
      .filter {
        it.returnType.isSubtypeOf(DeployFastExtension::class.createType())
      }
      .map { prop ->
        (prop as KProperty1<DeployFastExtension, DeployFastExtension>).get(this)
      }
  }

  var context: TaskContext? = null
    get() = field ?: throw UninitializedPropertyAccessException("AptExtension.context")
    set(value) {
      field = value
      tasks.context = value!!
    }

  /* Has state means extension represents a ONE process run on the host which state can be changed */
  open val hasState = true
  open val hasFacts = false

  open fun getInstalledState(): Boolean = TODO()

  // there can be several installations and running instances
  // each extension instance corresponds to ONE such process
  open fun getStatus(): ServiceStatus = TODO()

  fun play(): TaskResult = TODO()
}