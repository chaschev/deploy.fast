package fast.lang

import java.io.BufferedWriter

inline fun <T> nullForException(
  onError: ((Throwable) -> Unit) = { _ -> },
  block: () -> T): T? {

  return try {
    block.invoke()
  } catch (e: Throwable) {
    onError.invoke(e)
    null
  }
}

fun BufferedWriter.writeln(s: String) = write(s + "\n")