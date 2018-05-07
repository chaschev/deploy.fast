package fast.runtime

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

  suspend fun awaitKey(path: String, timeoutMs: Long = 600_000): Boolean {
    val startMs = System.currentTimeMillis()

    while(true) {
      if(globalMap.containsKey(path)) return true

      if(System.currentTimeMillis() - startMs > timeoutMs) return false

      delay(50)
    }
  }

  suspend fun <V> awaitCondition(path: String, predicate: (V) -> Boolean, timeoutMs: Long = 600_000): Boolean {
    val startMs = System.currentTimeMillis()

    while(true) {
      val value = globalMap.get(path) as V?

      if(value != null) {
        if(predicate(value)) return true
      }

      if(System.currentTimeMillis() - startMs > timeoutMs) return false

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

  data class DistributeResult(
    val job: Deferred<ITaskResult<*>?>,
    val dsl: DistributedJobDsl
  ) {
    suspend fun await() = (job.await() ?: okNull) as ITaskResult<Any?>
  }

  public inline fun <K, V> ConcurrentMap<K, V>.getOrPutFix(key: K, defaultValue: () -> V): V {
    // Do not use computeIfAbsent on JVM8 as it would change locking behavior
    val storedValue = this[key];

    if(storedValue != null) return storedValue

    val newValue = defaultValue()

    val previousStoredValue = putIfAbsent(key, newValue)

    return if(previousStoredValue != null) {
      println("already there: $previousStoredValue ${previousStoredValue.hashCode()}")
      previousStoredValue
    }else {
      println("first to put: $newValue")
      newValue
    }
  }

  /**
   * See usage (DSL) to understand how it works
   */
  suspend fun distribute(
    ctx: TaskContext<*, *, *>,
    block: DistributedJobDsl.() -> Unit,
    timeoutMs: Long = 600_000
  ): DistributeResult {
    val distributeKey = "distribute.${ctx.path}"

    val dsl = (globalMap.getOrPut(distributeKey, {
        println("inside the fucking block! ${ctx.address}")
        DistributedJobDsl().apply(block)
    }) as DistributedJobDsl)

    println("got dsl: $dsl, map: ${globalMap.hashCode()} - ${ctx.address} ")

    val myJob = dsl.getJob(ctx.session.host)

    val job = if(myJob == null) {
      asyncNoisy {
        logger.info { "distribute ${ctx.address} - no job, awaiting others ${ctx.path}" }
        awaitAllParties(distributeKey, timeoutMs)
        null
      }
    } else {
      logger.info { "distribute ${ctx.address} - job started ${ctx.path}" }

      val r = myJob(ctx)

      dsl.arrived(ctx.session.host, r)

      asyncNoisy {
        awaitAllParties(distributeKey, timeoutMs)  //todo: fix - await for leftover period of time
        r
      }
    }

    return DistributeResult(job, dsl)
  }

  private suspend fun awaitAllParties(path: String, timeoutMs: Long = 600_000) {
    awaitCondition<DistributedJobDsl>(path, {it.allPartiesArrived()}, timeoutMs)
    logger.info { "distribution: all parties arrived, $path" }
  }



  class DistributedJobDsl(
    val name: String? = null
  ) {
    internal val jobs: ArrayList<DistributedJobEntry> = ArrayList()

    lateinit var ctx: TaskContext<*, *, *>
    lateinit var ssh: SshProvider

    val jobResultMap = ConcurrentHashMap<String, TaskResult<Any>>()

    infix fun List<String>.with(block: suspend TaskContext<*,*,*>.() -> ITaskResult<*>) {
      jobs += DistributedJobEntry(this, block)
    }

    fun getJob(host: Host): (suspend TaskContext<*,*,*>.() -> ITaskResult<*>)? {
      jobs.forEach { (hosts,job) ->
        if(hosts.contains(host.address)) return job
      }

      return null
    }

    internal data class DistributedJobEntry(
      val hosts: List<String>,
      val block: (suspend TaskContext<*,*,*>.() -> ITaskResult<*>)
    )

    fun allPartiesArrived(): Boolean {
      val partyCount = size()
      return partyCount == jobResultMap.size
    }

    fun size() = jobs.sumBy { it.hosts.size }

    fun arrived(host: Host, result: ITaskResult<*>) {
      jobResultMap[host.address] = result as TaskResult<Any>

      logger.info { "distribute ${host.address} - arrived in a distribution task [${jobResultMap.size}/${size()}]" }
    }
  }

  companion object : KLogging() {

  }
}