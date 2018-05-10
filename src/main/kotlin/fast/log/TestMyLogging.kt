package fast.log

import fast.inventory.Host
import fast.log.LogLevel.*
import fast.log.RoutingAppender.Companion.routing
import java.io.File
import fast.log.OkLogContext.Companion.okLog
import honey.lang.readResource
import org.slf4j.LoggerFactory


//ok restrictTo
//ok routingAppender
//todo slf4j bindings
//todo slf4j integration
//todo log rotation
//todo nice & fast pattern layout


/*
* The idea for nice and fast layout
*
* printNextField
* printFieldOfWidth
* printField(obj, formatter)
*  -> DoubleFormatter - will tell
*/

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

      rules {
        restrict {
          applyTo("*")

          "asshole.logging.package" to warn
          "butterfly.poo.package" to trace
        }
      }

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
        intoAppenders(
          routing<Host> { host ->
            when (host) {
              null -> null
              else -> FileAppenderDsl(File("log/routes-${host.name}.out")) as Appender<Host, Any>
            }
          }
        )
      }

      any {
        withTransformer(PatternTransformer())
        intoAppenders(
          console1
        )
      }
    }.apply {
      debugMode = false
    }

    val simpleLogger = okLog.getLogger("simple")

    simpleLogger.info { "info" }

    val hostLoggerVm1 = okLog.getClassifiedLogger("simple3", Host("vm1"))
    val hostLoggerVm2 = okLog.getClassifiedLogger("simple3", Host("vm2"))

    hostLoggerVm1.log(info, null, { "vm1" })
    hostLoggerVm1.log(info, "console.out", { "vm1 con.out" })

    hostLoggerVm2.log(info, null, { "vm2" })
    hostLoggerVm2.log(info, "console.out", { "vm2 con.out" })

    simpleLogger.log(info, Host("vm1"), {"hi to vm1"})
    simpleLogger.log(info, Host("vm2"), {"hi to vm2"})
    simpleLogger.log(info, Host("vm3"), {"hi to vm3"})
    simpleLogger.log(info, Host("vm4"), {"hi to vm4"})

    okLog.getLogger("asshole.logging.package.xxx").info { "I am an asshole and I work" }
    okLog.getLogger("butterfly.poo.package.xxx").info { "I am a beautiful butterfly and I poo" }


//    println(this::class.java.readResource("/r/delme"))
    println(this::class.java.readResource("/META-INF/services/org.slf4j.spi.SLF4JServiceProvider"))



    LoggerFactory.getLogger("ok slf4j say hi").info("don't poo on slf4j")
  }


}

