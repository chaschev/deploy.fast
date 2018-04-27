package fast.ssh.command

import fast.ssh.tryFind

data class JavaVersion(
  val isOpenJDK: Boolean,
  val version: Version
) : Version {
  override val numbers: List<Comparable<Any>> by lazy { listOf(major(), build()) as List<Comparable<Any>> }

  fun major(): Int = version.numbers[0] as Int
  fun build(): Int = version.numbers[1] as Int

  fun isHigherThan(other: JavaVersion): Boolean =
    compareTo(other) >= 0

  override fun toString(): String =
    "${if (isOpenJDK) "openjdk" else "oraclejdk"} version ${version.asString()}"

  companion object {
    val JAVA_REGEX1 = """1.(\d+).\d+_(\d+)""".toRegex()


    internal fun parseJavaVersion(s: String): JavaVersion {
      val isOpenJDK = s.contains("openjdk")

      val g = JAVA_REGEX1.tryFind(s)

      val version = if (g != null) {
        listOf(g[1], g[2])
      } else {
        val major = """version "(\d+)""".toRegex().tryFind(s)!![1]
        val build = """build (\d+)""".toRegex().tryFind(s)!![1]
        listOf(major, build)
      }.map { it.toInt() }

      return JavaVersion(isOpenJDK, SimpleVersion(version as List<Comparable<Any>>))
    }

    internal fun parseJavacVersion(s: String): JavaVersion? {
      val myS = JAVA_REGEX1.tryFind(s) ?: return null

      return JavaVersion(true, Version.of(myS[1].toInt(), myS[2].toInt()))
    }
  }
}

interface Version : Comparable<Version> {
  companion object {
    private val REGEX = "[._\\-]".toRegex()

    val ZERO = Version.parse("0")

    fun parse(s: String): Version =
      SimpleVersion(s.split(REGEX).map {
        (it.toIntOrNull() ?: it) as Comparable<Any>
      })

    fun <T:Any> of(vararg numbers: Comparable<T>) = SimpleVersion(numbers.asList() as List<Comparable<Any>>)
  }


  val numbers: List<Comparable<Any>>

  fun isHigherThan(other: Version): Boolean =
    compareTo(other) >= 0

  operator fun get(i: Int): Int = numbers[i] as Int

  override fun compareTo(other: Version): Int {
    numbers.forEachIndexed { i, number1 ->
      if (other.numbers.size == i) return 1

      val number2 = other.numbers[i]

      if (number1.javaClass != number2.javaClass)
        throw IllegalArgumentException("We are trying to compare horses with courses: $number1 and $number2")

      val r = number1.compareTo(number2)

      if (r != 0) return r
    }

    if (numbers.size < other.numbers.size) return -1

    return 0
  }

  fun asString() = numbers.joinToString(".")
}

data class SimpleVersion(override val numbers: List<Comparable<Any>>) : Version {
  override fun toString(): String =
    "Version(${asString()})"
}