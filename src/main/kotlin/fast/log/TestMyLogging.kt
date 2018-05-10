package fast.log

import fast.inventory.Host
import java.io.File


//todo restrictTo
//todo routingAppender
//todo slf4j bindings
//todo slf4j integration
//todo log rotation


object TestMyLogging {
  @JvmStatic
  fun main(args: Array<String>) {
    val console1 = ConsoleAppender("console1")
    val console2 = ConsoleAppender("console2")

    val vm1 = FileAppenderDsl.fileAppender(File("log/vm1.txt")) {}
    val vm2 = FileAppenderDsl.fileAppender(File("log/vm2.txt")) {}

    OkLogContext.okLog = OkLogContext {
      /*starts("net.schmizz") {
        setLevel(warn)
      }*/

      /* starts("net.schmizz.sshj.DefaultConfig") {
         setLevel(error)
       }*/

      //each of these loggers will receive a classifier, i.e. a host during init from user
      /*allWithClassifier<Host> {
        withFilter<String, Any>(*//* filter 'console.out' messages *//*)
        withAppender(*//*.$host.out*//*)
      }*/

      all<Host> {
        classifyBase { it.name == "vm1" }
        classifyMsg<String> { c, msg -> c == "console.out" }
        filter<Any> { msg -> true }
        withTransformer(PatternTransformer())
        intoAppenders(
          console1,
          vm1
        )
      }

      //each of these loggers will receive a classifier, i.e. a host during init from user
      all<Host> {
        classifyBase { it.name == "vm1" }
        withTransformer(PatternTransformer())
        intoAppenders(console2, vm2)
      }

      all<Host> {
//        classifyBase { it.name == "vm1" }
        withTransformer(PatternTransformer())
        intoAppenders(RoutingAppender(
          Host::class.java,
          routes = {host, _ ->
            if(host == null)
              null
            else
              FileAppenderDsl(File("log/routes-${host.name}.out")) as Appender<Host, Any>
          }

        ))
      }
    }.apply {
      debugMode = true
    }

    val simpleLogger = OkLogContext.okLog.getLogger("simple")

    simpleLogger.info { "info" }

//    val hostLoggerVm1 = OkLogContext.okLog.getClassifiedLogger("simple3", Host("vm1"))
//    val hostLoggerVm2 = OkLogContext.okLog.getClassifiedLogger("simple3", Host("vm2"))

//    hostLoggerVm1.log(LogLevel.info, null, { "vm1" })
//    hostLoggerVm1.log(LogLevel.info, "console.out", { "vm1 con.out" })

//    hostLoggerVm2.log(LogLevel.info, null, { "vm2" })
//    hostLoggerVm2.log(LogLevel.info, "console.out", { "vm2 con.out" })

    simpleLogger.log(LogLevel.info, Host("vm1"), {"hi to vm1"})
    simpleLogger.log(LogLevel.info, Host("vm2"), {"hi to vm2"})
    simpleLogger.log(LogLevel.info, Host("vm3"), {"hi to vm3"})
    simpleLogger.log(LogLevel.info, Host("vm4"), {"hi to vm4"})
  }
}