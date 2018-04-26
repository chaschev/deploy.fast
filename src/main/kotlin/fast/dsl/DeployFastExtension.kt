package fast.dsl

import fast.runtime.AppContext
import fast.runtime.DeployFastDI.FAST
import fast.runtime.TaskContext
import org.kodein.di.generic.instance
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.isSubtypeOf

interface ExtensionConfig {
//  fun configure(): ExtensionConfig
}

class NoConfig: ExtensionConfig {

}

abstract class DeployFastApp(name: String) : DeployFastExtension<NoConfig>(name, {NoConfig()})

abstract class DeployFastExtension<CONF: ExtensionConfig>(
  val name: String,
  val config: (TaskContext) -> CONF
) {
  /* Named extension tasks */
  open val tasks: (TaskContext) -> NamedExtTasks = {
    NamedExtTasks(this as DeployFastExtension<ExtensionConfig>, it)
  }

  val app: AppContext by FAST.instance()

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