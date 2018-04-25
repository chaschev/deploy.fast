package fast.dsl

import fast.runtime.AllSessionsRuntimeContext
import fast.runtime.AppContext
import fast.runtime.TaskContext
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.isSubtypeOf

interface ExtensionConfig {
//  fun configure(): ExtensionConfig
}

class NoConfig: ExtensionConfig {

}

abstract class DeployFastApp(name: String, app: AppContext) : DeployFastExtension<NoConfig>(name, app, {NoConfig()})

abstract class DeployFastExtension<CONF: ExtensionConfig>(
  val name: String,
  val app: AppContext,
  val config: (TaskContext) -> CONF
) {
  /* Named extension tasks */
  open val tasks: (TaskContext) -> NamedExtTasks = {NamedExtTasks(it as DeployFastExtension<ExtensionConfig>)}

  lateinit var global: AllSessionsRuntimeContext

  internal fun init(allContext: AllSessionsRuntimeContext) {
    global = allContext

    for (ext in extensions) {
      ext.init(allContext)
    }
  }

  val extensions: List<DeployFastExtension<ExtensionConfig>> by lazy {
    val properties = this::class.declaredMemberProperties

    properties
      .filter {
        it.returnType.isSubtypeOf(DeployFastExtension::class.defaultType)
      }
      .map { prop ->
        (prop as KProperty1<DeployFastExtension<CONF>, DeployFastExtension<ExtensionConfig>>).get(this)
      }
  }

  /* Has state means extension represents a ONE process run on the host which state can be changed */
  open val hasState = true
  open val hasFacts = false


  fun play(): ITaskResult = TODO()
}