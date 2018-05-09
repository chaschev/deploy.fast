package fast.ssh

import fast.inventory.Host
import fast.log.KLogging
import fast.ssh.command.ConsoleLogging.sshOutLogger
import kotlinx.coroutines.experimental.*
import fast.ssh.command.Regexes
import fast.ssh.process.*
import honey.lang.getCurrentJob
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Not fully extracted interface
 */
interface IConsoleProcess : Cancellable {
  val console: Console
  val host: Host

  fun start(timeoutMs: Int = 60000, callback: ((console: Console) -> Unit)?): IConsoleProcess
  var result: ConsoleCommandResult?

  val startedAt: Long
  val egg: ProcessEgg

  suspend fun await(): IConsoleProcess
}

/**
 * Medium-level object representing a running newCommand in ssh or locally. It is holding:
 *
 *  job - a corresponding job, monitoring the state of the process
 *  console - i/o interaction and work done by the process
 *  process - a reference to a process. A local process or a remote ssh session with running process inside
 *
 *  mom - creates a baby process and is holding birth information. Command line and environment
 */
open class ConsoleProcess(
  override val egg: ProcessEgg
) : IConsoleProcess {
  companion object : KLogging()


  override var result: ConsoleCommandResult? = null

  override val startedAt = System.currentTimeMillis()

  private lateinit var process: BasicProcess

  lateinit var job: Deferred<IConsoleProcess>
  override lateinit var console: Console

  var readJob1: Job? = null
  var readJob2: Job? = null

  override suspend fun await(): IConsoleProcess {
    readJob1?.join()
    readJob2?.join()

    return job.await()
  }

  override val host
    get() = egg.host

  override fun start(timeoutMs: Int, callback: ((console: Console) -> Unit)?): ConsoleProcess {
    logger.debug { "starting a new job: ${describeMe()} with timeout ${timeoutMs}ms" }

    job = asyncNoisy {
      process = egg.giveBirth(getCurrentJob())

      logger.info(host.marker, "started $egg")

      val cmd = "\n> ${egg.cmd}\n"

      sshOutLogger.debug(host.marker, cmd)

      val writer = process.stdin.bufferedWriter()

      val stderr = process.stderr
      val stdout = process.stdout

      console = Console(writer, this@ConsoleProcess)

      // todo: change to non-blocking coroutines
      readJob1 = launch(CommonPool) {
        while (true) {
          console.newOut = tryRead(stdout, console.stdout)

          if (callback != null && !console.newOut.isEmpty()) {
            callback(console)
          }

          //put it here, so after delay there is additional check for value
          if(!(isActive && job.isActive)) break

          delay(10)
        }
      }

      readJob2 = launch(CommonPool) {
        while (true) {
          console.newErr = tryRead(stderr, console.stderr)

          if (callback != null && !console.newErr.isEmpty()) {
            callback(console)
          }

          //put it here, so after delay there is additional check for value
          if(!(isActive && job.isActive)) break

          delay(10)
        }
      }

      while (isActive) {
        val timeMs = System.currentTimeMillis() - startedAt
        if (timeMs > timeoutMs) {
          logger.info(host) { "timeout ${timeoutMs}ms for '$egg'" }

          result = ConsoleCommandResult(console = console, isTimeout = true, timeMs = timeMs)

          try {
            process.cancel()
          } finally {
            result = process.getResult(console, process.isAlive())
          }

          break
        }

        if (process.isEOF() || !process.isAlive()) {
          //wait for read/write jobs - this is a must!
          delay(30)

          val r = process.getResult(console)

          result = r

          logger.info { "command finished with result $r,\n" +
            "  stdout=${r.console.stdout.cuteCutLast(100).trim()}\n" +
            "  error=${r.console.stderr.cuteCutLast(100).trim()}\n" +
            "  command=${egg.toString().cuteSubstring(0, 40)}\n" }

          break
        }

        delay(30)
      }

      if(result == null) {
        logger.warn { "some unknown error occurred for ${describeMe()}" }
        result = ConsoleCommandResult(console, null, false, false)
      }

      readJob1?.cancel()
      readJob2?.cancel()

      if (process.isAlive() && !isActive) {
        process.cancel()
      }

      console.result = result!!

      this@ConsoleProcess
    }

//    logger.debug { "job started: ${describeMe()}" }

    return this
  }

  private fun describeMe() = egg.toString().cuteCut(60)

  override fun cancel() {
    readJob1?.cancel()
    readJob2?.cancel()
    job.cancel()
  }

  private fun tryRead(input: InputStream, sb: StringBuilder): String {
    if (input.available() <= 0) {
      return ""
    }

    val baos = ByteArrayOutputStream(4096)

    val buf = ByteArray(input.available())
    logger.trace { "reading ${buf.size} bytes..." }

    val bytesRead = input.read(buf)

    if (bytesRead > 0) {
      baos.write(buf, 0, bytesRead)

      logger.trace { "read $bytesRead bytes" }

      val newText = baos.toString().replace(Regexes.NEW_LINE, "\n")

      sb.append(newText)

      // don't care about -1 now
      //            LoggerFactory.getLogger("log").trace("nonBlockingCopy: {}", read);

      return newText
    } else {
      return ""
    }
  }
}