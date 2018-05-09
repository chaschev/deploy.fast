package fast.lang

import java.io.BufferedWriter

inline fun <T> nullForException(
  exception: Class<*>? = null,
  onError: ((Throwable) -> Unit) = { _ -> },
  block: () -> T): T? {

  return try {
    block.invoke()
  } catch (e: Throwable) {
    if(exception == null) {
      onError.invoke(e)
      null
    } else {
      if(e.javaClass.isAssignableFrom(exception)) {
        onError.invoke(e)
        null
      } else {
        throw e
      }
    }
  }
}

fun BufferedWriter.writeln(s: String) = write(s + "\n")