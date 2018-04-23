package fast.ssh

import kotlinx.coroutines.experimental.*
import mu.KotlinLogging
import net.schmizz.sshj.connection.channel.direct.Session
import kotlin.coroutines.experimental.CoroutineContext

//TODO: this might be called too many times
//TODO: can be synced through internal openCloseLock
fun Session.Command.tryClose() {
  if (!isOpen) return

  val closeJob = asyncNoisy {
    close()
  }

  asyncNoisy {
    delay(50)
    if (!closeJob.isCompleted) {
      delay(300)
    }
    if (!closeJob.isCompleted) {
      GenericSshSession.logger.info { "couldn't close command in 300ms" }
    }
  }
}

fun CharSequence.cuteCut(length: Int): String = cuteSubstring(0, length)

fun CharSequence.cuteSubstring(from: Int, to: Int): String {
  check(from >= 0, { "from must be >=0, got $from" })

  var l = if (to == -1) length else to

  if (l >= length) l = length

  //nothing to cut
  if (l == length) return substring(from, l)

  l -= 3

  if (l < from) l = from

  return substring(from, l) + "..."
}

val logger = KotlinLogging.logger("badger.core")

fun <T> asyncNoisy(
  block: suspend CoroutineScope.() -> T
): Deferred<T> {
  return async(context = CommonPool, block = {
    try {
      return@async block.invoke(this)
    } catch (e: Throwable) {
      logger.info(e, { "exception in async block" })
      throw e
    }
  })
}


fun Regex.tryFind(s: String): List<String>? =
  find(s)?.groups?.map { it?.value ?: "null" }
