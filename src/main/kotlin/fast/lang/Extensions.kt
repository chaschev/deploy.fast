package fast.lang

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
