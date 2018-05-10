package fast.log

/**
 * @param routes Returns null when there is no route. Null/no route for null input classifier is ok,
 *                 null input log level means "any" log level, so null/noRoute return is not ok
 */
class RoutingAppender<C, O>(
  val classifier: Class<C>,
  override val name: String = "${classifier.simpleName}.routing.appender",
  private val routes: (C?, LogLevel) -> Appender<C, O>?
) : Appender<C, O> {
  private val routeMap = ArrayList<Triple<C, LogLevel?, Appender<C, O>>>()

  override fun append(obj: O) {
    TODO("not implemented")
  }

  override fun append(obj: O, classifier: C?, level: LogLevel) {
    val appender = getRoute(classifier, level)

    if(appender == noRoute) return

    appender.append(obj, classifier, level)
  }

  override fun transform(
    transformer: Transformer<C, O>,
    classifier: C?,
    obj: O,
    level: LogLevel
  ) {
    val appender = getRoute(classifier, level)

    if(appender == noRoute) return

    appender.transform(transformer, classifier, obj, level)
  }

  private fun getRoute(classifier: C?, level: LogLevel): Appender<C, O> {
    if(classifier != null && !this.classifier.isInstance(classifier)) return noRoute as Appender<C, O>

    var r = tryFindRoute(classifier, level)

    if (r != notFound) return r

    return synchronized(this) {
      var r = tryFindRoute(classifier, level)

      if (r != notFound) {
        r
      } else {
        val appender = routes(classifier, level) ?: noRoute

        routeMap.add(Triple(
            classifier as C, level, appender
          ) as Triple<C, LogLevel?, Appender<C, O>>
        )
        appender
      } as Appender<C, O>

    }
  }

  override fun supportsTransform(): Boolean = true

  private fun tryFindRoute(classifier: C?, level: LogLevel): Appender<C, O> {
    for ((c, l, a) in routeMap) {
      if ((c == classifier) && (level == l || l == null)) {
        return a
      }
    }

    return notFound as Appender<C, O>
  }

  companion object {
    private val notFound = ConsoleAppender("noAppender")
    val noRoute = ConsoleAppender("noRoute")

    inline fun <reified C> routing(
//      classifier: Class<C>,
      crossinline routes: (C?) -> Appender<C, Any>?
    ): RoutingAppender<C, Any> {
      return RoutingAppender(C::class.java, routes = { host, _ ->
        routes(host)
      })
    }
  }
}