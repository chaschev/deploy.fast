package fast.runtime



import fast.inventory.Host
import fast.log.*
import fast.log.LogLevel.*
import fast.log.RoutingAppender.Companion.routing
import java.io.File
import fast.log.OkLogContext.Companion.okLog
import honey.lang.readResource
import org.slf4j.LoggerFactory
import fast.log.OkLogContext
import fast.log.LoggerFactoryDSL

fun configDeployFastLogging() {
  val console = ConsoleAppender("console", true)

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

        "net.schmizz" to warn
        "net.schmizz.sshj.DefaultConfig" to error
      }
    }

    appenders(listOf(
      routing<Host>("routing.out") { host ->
        FileAppenderDsl(File("log/routes-${host?.name}.out")) as Appender<Host, Any>
      },

      routing<Host>("routing.log") { host ->
        FileAppenderDsl(File("log/routes-${host?.name}.log")) as Appender<Host, Any>
      }
    ))

    //all messages having out marker
    all<Host> {
      classifyMsg { c: Any? -> c == "ssh.out" }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out")
      )
    }

    //all messages having host specified from any of the loggers
    all<Host> {
      classifyMsg { c: Host? -> c != null }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out")
      )
    }

    //all loggers having host specified
    all<Host> {
      classifyBase { it != null  }
      withTransformer(PatternTransformer())
      intoAppenders(
        ref("routing.log")
      )
    }

    //default messaging processing: no classifier specified - dump to console

    any {
      classifyBase { it == null }
      classifyMsg { c: Any? -> c == null}
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    /* Additionally, add logs of level info from a host vm1 to console */
    all<Host> {
      classifyMsg { c: Any? -> c == null}
      filterLevel(info)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    all<Host> {
      classifyBase { it?.name == "vm1" }
      filterLevel(info)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

  }.apply {
    debugMode = false
  }}

