package fast.ssh.command

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
        "${if(isOpenJDK) "openjdk" else "oraclejdk"} version ${version.asString()}"
}

interface Version : Comparable<Version> {
    companion object {
        private val REGEX = "[._\\-]".toRegex()

        val ZERO = Version.parse("0")

        fun parse(s: String): Version =
            SimpleVersion(s.split(REGEX).map {
                (it.toIntOrNull() ?: it) as Comparable<Any>
            })
    }


    val numbers: List<Comparable<Any>>

    fun isHigherThan(other: Version): Boolean =
        compareTo(other) >= 0

    operator fun get(i: Int): Int = numbers[i] as Int

    override fun compareTo(other: Version): Int {
        numbers.forEachIndexed { i, number1 ->
            if(other.numbers.size == i) return 1

            val number2 = other.numbers[i]

            if(number1.javaClass != number2.javaClass)
                throw IllegalArgumentException("We are trying to compare horses with courses: $number1 and $number2")

            val r = number1.compareTo(number2)

            if(r != 0) return r
        }

        if(numbers.size < other.numbers.size) return -1

        return 0
    }

    fun asString() = numbers.joinToString(".")
}

data class SimpleVersion(override val numbers: List<Comparable<Any>>) : Version {
    override fun toString(): String =
        "Version(${asString()})"
}