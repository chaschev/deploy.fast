package fast.dsl

import fast.runtime.*
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
  companion object {
    val noConfig = NoConfig()
  }
}

typealias ChildTaskContext<EXT, CONF> = TaskContext<Any, EXT, CONF>

//abstract class AnyExtension<CONF: ExtensionConfig>(name: String, conf: CONF) : DeployFastExtension<AnyExtension<CONF>, CONF>(name, {conf})
typealias AnyExtension<CONF> = DeployFastExtension<*, CONF>

abstract class DeployFastApp<APP: DeployFastExtension<APP, NoConfig>>(name: String) : DeployFastExtension<APP, NoConfig>(name, {NoConfig()})

abstract class DeployFastExtension<EXT: DeployFastExtension<EXT, CONF>, CONF: ExtensionConfig>(
  val name: String,
  val config: (ChildTaskContext<EXT, CONF>) -> CONF  // child task will have any return argument
) {
  /**
   * Named extension tasks
   * Each call will create a child context from parent for executing extension's tasks
    *
   * */
  open val tasks: (AnyTaskContext) -> NamedExtTasks<EXT, CONF> = {
    NamedExtTasks(this as EXT, it)
  }

  val app: AppContext by FAST.instance()

  val asTask by lazy {
    Task<Any, EXT, CONF>(name, extension = this as EXT)
  }

  /*val extensions: List<DeployFastExtension<ExtensionConfig>> by lazy {
    val properties = this::class.declaredMemberProperties

    properties
      .filter {
        it.returnType.isSubtypeOf(DeployFastExtension::class.defaultType)
      }
      .map { prop ->
        (prop as KProperty1<DeployFastExtension<CONF>, DeployFastExtension<ExtensionConfig>>).get(this)
      }
  }*/

  /* Has state means extension represents a ONE process run on the host which state can be changed */
  open val hasState = true
  open val hasFacts = false
}