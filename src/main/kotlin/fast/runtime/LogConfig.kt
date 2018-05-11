package fast.runtime



import fast.inventory.Host
import fast.log.*
import fast.log.LogLevel.*
import fast.log.RoutingAppender.Companion.routing
import java.io.File
import fast.log.OkLogContext

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

        "net.schmizz.sshj.DefaultConfig" to ERROR
        "net.schmizz" to WARN
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

    //all loggers having out marker
    all {
      classifyBase { c: Any? -> c  == "ssh.out" }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out")
      )
    }

    //all messages having host specified from any of the loggers
    all {
      classifyMsg { it is Host }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out")
      )
    }

    //all loggers having host specified
    all {
      classifyBase { it is Host }
      withTransformer(PatternTransformer())
      intoAppenders(
        ref("routing.log")
      )
    }

    //default messaging processing: no classifier specified - dump to console

    any {
      filterBase {
        !it.name.endsWith(".out") &&
        !it.name.endsWith(".err") &&
        !it.name.endsWith(".log")
      }
      classifyBase { it == null }
      classifyMsg { c: Any? -> c == null}
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    /* Additionally, add logs of level info from a host vm1 to console */
    all {
      classifyMsg { it is Host && it.name == "vm1"}
      filterLevel(INFO)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    all {
      classifyBase { it is Host && it.name == "vm1" }
      filterLevel(INFO)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

  }.apply {
    debugMode = false
  }}

