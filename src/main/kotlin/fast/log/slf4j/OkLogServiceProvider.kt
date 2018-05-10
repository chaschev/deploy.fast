package fast.log.slf4j


import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class OkLogServiceProvider : SLF4JServiceProvider {

  private val loggerFactory: ILoggerFactory = OkLogLoggerFactory()
  private val markerFactory: IMarkerFactory = BasicMarkerFactory()
  private val mdcAdapter: MDCAdapter = NOPMDCAdapter()

  override fun getLoggerFactory(): ILoggerFactory {
    return loggerFactory
  }

  override fun getMarkerFactory(): IMarkerFactory {
    return markerFactory
  }

  override fun getMDCAdapter(): MDCAdapter {
    return mdcAdapter
  }

  override fun getRequesteApiVersion(): String {
    return REQUESTED_API_VERSION
  }


  override fun initialize() {
  }

  companion object {

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified with each major release.
     */
    var REQUESTED_API_VERSION = "1.8.99" // !final
  }

}