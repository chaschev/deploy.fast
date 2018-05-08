package fast.runtime

import fast.api.DeployFastExtension
import fast.api.ExtensionConfig
import fast.api.ITaskResult
import fast.dsl.TaskResult
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.TaskResult.Companion.okNull
import fast.inventory.Host
import fast.ssh.SshProvider
import fast.ssh.asyncNoisy
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.delay
import mu.KLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class GlobalMap {
  private val globalMap = ConcurrentHashMap<String, Any>()

  /**
   * Party coordination could be done in a similar fashion.
   *
   * I.e. can await for an
   *  (taskKey, AtomicInteger) value,
   *  or a certain state of parties
   *  shared result state isCompleted() = true. Parties report to this result
   */

  operator fun get(key: String): Any? = globalMap[key]

  suspend fun awaitKey(path: String, timeoutMs: Long = 600_000): Any? {
    val startMs = System.currentTimeMillis()

    while (true) {
      if (globalMap.containsKey(path)) return globalMap[path]!!

      if (System.currentTimeMillis() - startMs > timeoutMs) return null

      delay(50)
    }
  }

  suspend fun <V> awaitCondition(path: String, predicate: (V) -> Boolean, timeoutMs: Long = 600_000): V? {
    val startMs = System.currentTimeMillis()

    while (true) {
      val value = globalMap.get(path) as V?

      if (value != null) {
        if (predicate(value)) return value
      }

      if (System.currentTimeMillis() - startMs > timeoutMs) return null

      delay(50)
    }
  }

  @Deprecated("it is unusable - need to implement safe getOrPut")
  suspend fun <R> runOnce(path: String, block: suspend () -> R): Deferred<R> {
    val r = globalMap.getOrPut(path, {
      asyncNoisy {
        block()
      }
    })

    return r as Deferred<R>
  }

  data class DistributeResult<EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
    val job: Deferred<ITaskResult<*>?>,
    val dsl: DistributedJobDsl<EXT, EXT_CONF>?
  ) {
    suspend fun await() = (job.await() ?: okNull) as ITaskResult<Any?>
  }

  public inline fun <K, V> ConcurrentMap<K, V>.getOrPutFix(key: K, defaultValue: () -> V): V {
    // Do not use computeIfAbsent on JVM8 as it would change locking behavior
    val storedValue = this[key];

    if (storedValue != null) return storedValue

    val newValue = defaultValue()

    val previousStoredValue = putIfAbsent(key, newValue)

    return if (previousStoredValue != null) {
      println("already there: $previousStoredValue ${previousStoredValue.hashCode()}")
      previousStoredValue
    } else {
      println("first to put: $newValue")
      newValue
    }
  }

  /*suspend fun awaitForDistributeJob(name: String, ctx: TaskContext<*, *, *>): Deferred<Any> {
    val distributeKey = "distribute.$name.${ctx.path}"

    return awaitKey(distributeKey)
  }*/

  /**
   * See usage (DSL) to understand how it works
   *
   * Returns a job to await for completion. Inactive parties register with null dsl.
   *
   * TODO: it is not so clear if hosts defined in dsl are the same as hosts that provide non-null dsl.
   *  Fuck it for now
   *
   * @param await True means the party can't do this orchestration.
   *              I.e. it doesn't have artifacts, so it doesn't know which files to copy
   *              So, it can't provide definition of the task, which is dsl
   *
   *              TODO: clean up the mess
   */
  suspend fun <EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig> distribute(
    name: String,
    ctx: TaskContext<*, EXT, EXT_CONF>,
    block: DistributedJobDsl<EXT, EXT_CONF>.() -> Unit,
    await: Boolean = false,
    timeoutMs: Long = 600_000
  ): DistributeResult<EXT, EXT_CONF> {
    logger.info { "distribute $name ${ctx.address} - starting" }

    val distributeKey = "distribute.$name.${ctx.path}"

    val dsl = if(await) null else
      (globalMap.getOrPut(distributeKey, {
        DistributedJobDsl<EXT, EXT_CONF>(name).apply(block)
      }) as DistributedJobDsl<EXT, EXT_CONF>)

//    println("got dsl: $dsl, map: ${globalMap.hashCode()} - ${ctx.address} ")

    val myJob = dsl?.getJob(ctx.session.host)

    val job = if (myJob == null) {
      asyncNoisy {
        logger.info { "distribute $name ${ctx.address} - no job, awaiting others ${ctx.path}" }
        awaitAllParties(name, distributeKey, timeoutMs)
        null
      }
    } else {
      logger.info { "distribute $name ${ctx.address} - job started ${ctx.path}" }

      val r = myJob(ctx)

      dsl.arrived(ctx.session.host, r)

      asyncNoisy {
        awaitAllParties(name, distributeKey, timeoutMs)  //todo: fix - await for leftover period of time
        r
      }
    }

    //note: non-null dsl can be returned in a job result

    return DistributeResult(job, dsl)
  }

  private suspend fun awaitAllParties(name: String, path: String, timeoutMs: Long = 600_000) {
    awaitCondition<DistributedJobDsl<*, *>>(path, { it.allPartiesArrived() }, timeoutMs)
    logger.info { "distribute $name all parties arrived, $path" }
  }

  operator fun set(s: String, value: Any): Any? {
    return globalMap.put(s, value)
  }


  class DistributedJobDsl<EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig> (
    val name: String
  ) {
    internal val jobs: ArrayList<DistributedJobEntry<EXT, EXT_CONF>> = ArrayList()

    lateinit var ctx: TaskContext<*, *, *>
    lateinit var ssh: SshProvider

    val jobResultMap = ConcurrentHashMap<String, TaskResult<Any>>()
    var await: Boolean = false

    infix fun List<String>.with(block: suspend TaskContext<*, EXT, EXT_CONF>.() -> ITaskResult<*>) {
      jobs += DistributedJobEntry(this, block )
    }

    fun getJob(host: Host): (suspend TaskContext<*, EXT, EXT_CONF>.() -> ITaskResult<*>)? {
      println("job for $host in $jobs")
      jobs.forEach { (hosts, job) ->
        if (hosts.contains(host.address)) return job
      }

      return null
    }

    internal data class DistributedJobEntry<EXT: DeployFastExtension<EXT, EXT_CONF>, EXT_CONF: ExtensionConfig>(
      val hosts: List<String>,
      val block: (suspend TaskContext<*, EXT, EXT_CONF>.() -> ITaskResult<*>)
    )

    fun allPartiesArrived(): Boolean {
      val partyCount = size()
      return partyCount == jobResultMap.size
    }

    fun size() = jobs.sumBy { it.hosts.size }

    fun arrived(host: Host, result: ITaskResult<*>) {
      jobResultMap[host.address] = result as TaskResult<Any>

      logger.info { "distribute $name ${host.address} - arrived in a distribution task [${jobResultMap.size}/${size()}]" }
    }
  }

  companion object : KLogging() {

  }
}