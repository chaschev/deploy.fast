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
//ok slf4j bindings
//ok slf4j integration
//todo log rotation
//ok nice & fast pattern layout

//ok {} support
//ok FIX
//ok fix restrictions
//todo exceptions
//todo finish SLF4j Logger

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

          "asshole.logging.package" to WARN
          "butterfly.poo.package" to TRACE
        }
      }

      all {
        classifyBase { it is Host && it.name == "vm1" }
        classifyMsg { it == "console.out" }
        filter<Any> { true }
        withTransformer(PatternTransformer())
        intoAppenders(
          console1,
          vm1
        )
      }

      //each of these loggers will receive a classifier, i.e. a host during init from user
      all {
        classifyBase { it is Host && it.name == "vm1" }
        withTransformer(PatternTransformer())
        intoAppenders(console2, vm2)
      }

      all {
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

    hostLoggerVm1.log(INFO, lazyMsg = { "vm1" })
    hostLoggerVm1.log(INFO, "console.out", lazyMsg = { "vm1 con.out" })

    hostLoggerVm2.log(INFO, null, lazyMsg = { "vm2" })
    hostLoggerVm2.log(INFO, "console.out", lazyMsg = { "vm2 con.out" })

    simpleLogger.log(INFO, Host("vm1"), lazyMsg = { "hi to vm1" })
    simpleLogger.log(INFO, Host("vm2"), lazyMsg = { "hi to vm2" })
    simpleLogger.log(INFO, Host("vm3"), lazyMsg = { "hi to vm3" })
    simpleLogger.log(INFO, Host("vm4"), lazyMsg = { "hi to vm4" })

    okLog.getLogger("asshole.logging.package.xxx").info { "I am an asshole and I work" }
    okLog.getLogger("butterfly.poo.package.xxx").info { "I am a beautiful butterfly and I poo" }


//    println(this::class.java.readResource("/r/delme"))
    println(this::class.java.readResource("/META-INF/services/org.slf4j.spi.SLF4JServiceProvider"))


    LoggerFactory.getLogger("ok slf4j say hi").info("don't poo on slf4j")
  }


}

