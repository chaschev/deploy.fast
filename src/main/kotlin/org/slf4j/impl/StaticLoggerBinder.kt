package org.slf4j.impl

import fast.log.slf4j.OkLogLoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

class StaticLoggerBinder : LoggerFactoryBinder {
  @get:JvmName("_loggerFactory")
  private val loggerFactory = OkLogLoggerFactory()

  override fun getLoggerFactory() = loggerFactory

  override fun getLoggerFactoryClassStr() = Companion.loggerFactoryClassStr

  companion object {
    val _singleton = StaticLoggerBinder()

    @JvmStatic
    fun getSingleton() = _singleton

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified with each major release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    @JvmField
    var REQUESTED_API_VERSION = "1.7.25" // !final

    private val loggerFactoryClassStr = OkLogLoggerFactory::class.java.name
  }
}