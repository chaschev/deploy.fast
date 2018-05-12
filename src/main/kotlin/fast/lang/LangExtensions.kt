package honey.lang

import com.google.common.collect.Iterators
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.AbstractList


fun <T, R> Iterator<T>.map(f: (T) -> R) = Iterators.transform(this, f as (input: T?) -> R?)
fun <T> Iterator<Iterator<T>>.flatten() = Iterators.concat(this)
fun <T> Iterator<T>.peeking() = Iterators.peekingIterator(this)
fun <T> Iterator<T>.concat(other: Iterator<T>) = Iterators.concat(this, other)

fun <K, V> Iterable<MutableMap.MutableEntry<K, V>>.toMap(): HashMap<K, V> {
  val map = HashMap<K, V>()
  val iterator = this.iterator()

  for (e in iterator) {
    map[e.key] = e.value
  }

  return map
}

fun <K, V, M : MutableMap<K, V>> Iterable<MutableMap.MutableEntry<K, V>>.toMap(map: M): M {
  val iterator = this.iterator()

  for (e in iterator) {
    map[e.key] = e.value
  }

  return map
}

fun CharSequence.substringBetween(prefix: String, firstSuffix: String): String {
  var i1 = indexOf(prefix)
  if (i1 == -1) i1 = -prefix.length

  var i2 = indexOf(firstSuffix, i1 + prefix.length)

  if (i2 == -1) i2 = length

  return substring(i1 + prefix.length, i2)
}

fun Instant.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()) = LocalDateTime.ofInstant(this, zone)
fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(this).toLocalDateTime(zone)


fun <E> List<E>.prepend(vararg elements: E): List<E> {
  return CompositeUnmodifiableList(elements.asList(), this)
}

fun <E> List<E>.prepend(other: List<E>): List<E> {
  return CompositeUnmodifiableList(other, this)
}

fun <E> List<E>.append(vararg elements: E): List<E> {
  return CompositeUnmodifiableList(this, elements.asList())
}

fun <E> List<E>.append(other: List<E>): List<E> {
  return CompositeUnmodifiableList(this, other)
}

class CompositeUnmodifiableList<E>(
  private val list1: List<E>,
  private val list2: List<E>
) : AbstractList<E>() {
  override val size: Int
    get() = list1.size + list2.size

  override fun get(index: Int): E {
    return if (index < list1.size) {
      list1[index]
    } else list2[index - list1.size]
  }
}

fun <E> Set<E>.smartUnion(other: Set<E>): Set<E> {
  return CompositeUnmodifiableSet(other, this)
}

class CompositeUnmodifiableSet<E>(
  private val set1: Set<E>,
  private val set2: Set<E>
) : AbstractSet<E>() {
  override fun iterator(): Iterator<E> {
    return set1.iterator().concat(set2.iterator())
  }

  override val size: Int
    get() = set1.size + set2.size


}

inline fun <E, R> MutableList<E>.mapReplace(map: (E) -> R): MutableList<R> {
  for(i in 0 until size) {
    this[i] = map(this[i]) as E
  }

  return this as MutableList<R>
}

inline fun CharSequence.scanForIndex(startAt: Int, predicate: (ch: Char) -> Boolean): Int? {
  for(index in startAt until length) {
    if(predicate(this[index])) return index
  }

  return null
}


inline fun CharSequence.scanForIndexExt(startAt: Int, predicate: (ch: Char, index:Int, length: Int) -> Boolean): Int? {
  for(index in startAt until length) {
    if(predicate(this[index], index, length)) return index
  }

  return null
}


fun CoroutineScope.getCurrentJob() = coroutineContext[Job]!!

fun Iterable<String>.joinSpace() = joinToString(" ")
fun Iterable<String>.joinComma() = joinToString(", ")
fun Iterable<String>.joinEol() = joinToString("\n")

fun CharSequence.startsWithAny(prefixes: Iterable<out String>): Boolean {
  return prefixes.find { this.startsWith(it) } != null
}

fun CharSequence.startsWithAny(prefixes: Array<out String>): Boolean {
  return prefixes.find { this.startsWith(it) } != null
}

fun CharSequence.endsWithAny(vararg suffixes: String): Boolean {
  return suffixes.find { this.endsWith(it) } != null
}

fun ByteArray.text() = String(this)

fun Class<out Any>.readResource(path: String) =
  getResourceAsStream(path).readBytes().text()