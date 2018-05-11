package fast.ssh

import fast.inventory.Host
import fast.log.KLogging
import fast.log.OkLogging
import kotlinx.coroutines.experimental.*
import net.schmizz.sshj.connection.channel.direct.Session

//TODO: this might be called too many times
//TODO: can be synced through internal openCloseLock
fun Session.Command.tryClose(host: Host) {
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
      GenericSshSession.logger.info(host) { "couldn't close command in 300ms" }
    }
  }
}

fun CharSequence.cuteCut(length: Int): String = cuteSubstring(0, length)
fun CharSequence.cuteCutLast(length: Int): String = cuteSubstring(Math.max(0, this.length - length), this.length, false)

fun CharSequence.cuteSubstring(from: Int, to: Int, multiOnRight: Boolean = true): String {
  check(from >= 0, { "from must be >=0, got $from" })

  var l = if (to == -1) length else to

  if (l >= length) l = length

  //nothing to cut
  if (l == length) return substring(from, l)

  // prefix vs suffix
  if(multiOnRight) {
    val lBefore = l
    l -= 3

    // for a very small string, less than '...'
    if (l < from) return substring(from, lBefore)

    return substring(from, l) + "..."
  }else {
    var updFrom = from
    updFrom += 3

    //there is probably a bug here
    if (l < updFrom) return substring(from, l)

    return "..." + substring(updFrom, l)
  }

}

val logger = OkLogging().logger

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
