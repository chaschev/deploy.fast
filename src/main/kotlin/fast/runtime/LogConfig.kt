package fast.runtime


import fast.inventory.Host
import fast.inventory.Inventory
import fast.lang.writeln
import fast.log.*
import fast.log.LogLevel.*
import fast.log.RoutingAppender.Companion.routing
import java.io.File
import fast.log.OkLogContext
import honey.lang.endsWithAny
import org.kodein.di.generic.instance

fun configDeployFastLogging() {
  val console = ConsoleAppender("console", true)

  OkLogContext.okLog = OkLogContext {
    val inventory = DeployFastDI.FASTD.instance<Inventory>()
    val host1 = inventory.activeHosts[0].name

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
    all("base.routing.out") {
      classifyBase { c: Any? -> c == "ssh.out" }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out"),
        ref("routing.log")
      )
    }

    all("msg.routing.out") {
      classifyMsg { it is Host }
      withTransformer(PlainTextTransformer())
      intoAppenders(
        ref("routing.out")

      )
    }

    //all loggers having host specified
    all("base.routing.log") {
      classifyBase { it is Host }
      withTransformer(PatternTransformer())
      intoAppenders(
        ref("routing.log")
      )
    }

    all("msg.routing.log") {
      filterBase {
        !it.name.endsWithAny(".out", ".err", ".log")
      }
      classifyMsg { it is Host }
      withTransformer(
        // msg.routing.out will dump raw  console output into the same file, so we
        // need to separate log messages with a new line symbol
        BeforeAfterTransformer(PatternTransformer(), before = { it.newLine() })
      )
      intoAppenders(
        ref("routing.log")
      )
    }

    //default messaging processing: no classifier specified - dump to console
    any("default") {
      filterBase {
        !it.name.endsWithAny(".out", ".err", ".log")
      }
      classifyBase { it == null }
      classifyMsg { c: Any? -> c == null }
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    /* Additionally, add logs of level info from a host vm1 to console */
    all("$host1.out") {
      filterBase { it.name == "ssh.out" }
      classifyMsg {
        it is Host && it.name == host1
      }
      withTransformer(
        BeforeAfterTransformer(PlainTextTransformer(),
          before = { it.writeln("$host1: ") }
        )
      )
      intoAppenders(console)
    }

    /* Additionally, add logs of level info from a host vm1 to console */
    all("$host1.msg") {
      classifyMsg {
        it is Host && it.name == host1
      }
      filterLevel(INFO)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

    all("$host1.base") {
      classifyBase { it is Host && it.name == host1 }
      filterLevel(INFO)
      withTransformer(PatternTransformer())
      intoAppenders(console)
    }

  }.apply {
    debugMode = false
  }
}
