package fast.inventory

import java.io.File
import java.util.*

open class ConfigProps(private val props: Properties) {
  constructor(file: File) : this(loadProperties(file))

  //check my props, if null then def value, then env (if configured)
  operator fun get(name: String, defaultValue: String? = null, envFallback:Boolean = true): String? {
    return props.getProperty(name, defaultValue) ?:
      if(envFallback) System.getenv(name) else null
  }

  fun keys() = props.keys.map {it.toString()}
  fun value() = props.values.map {it.toString()}
  fun entries() = props.entries.map {it.key.toString() to it.value.toString()}

  companion object {
    fun new(file: File): ConfigProps {
      val props = loadProperties(file)

      return ConfigProps(props)
    }

    private fun loadProperties(file: File): Properties {
      val props = Properties()
      if (!file.exists()) {
        throw NoSuchFileException(file, reason = "you need to initialize with ${file.absolutePath}")
      }

      file.inputStream().use {
        props.load(it)
      }

      return props
    }

    fun new(props: Properties) = ConfigProps(props)
  }
}