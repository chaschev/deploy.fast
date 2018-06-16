package fast.sample.perf

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

//50k couroutines lead to 2 loaded at 100% on Macbook Pro '17
object CoroutineMassAwait {
  @JvmStatic
  fun main(args: Array<String>) {
    val startedAtMs = System.currentTimeMillis()
    runBlocking {
      (1..50000).map { jobIndex ->
        async {
          repeat(60) {
            if (jobIndex == 50) {
              println("hi from $jobIndex: $it")
              println("${System.currentTimeMillis() - startedAtMs} vs ${(it + 1) * 500}")
            }
            delay(6000)
          }
        }
      }.forEach {
        it.join()
      }
    }
  }
}

object TestExecutor {
  @JvmStatic
  fun main(args: Array<String>) {
    val cutor = Executors.newFixedThreadPool(10000)

    repeat(100000) { jobIndex ->
      cutor.submit(Callable {
        runBlocking {
          repeat(10) {
            println("hi from $jobIndex: $it")
            delay(500)
          }
        }
      })
    }

    cutor.shutdown()
    cutor.awaitTermination(30, TimeUnit.SECONDS)
  }
}


object CoroutineChannelsAwait {
  @JvmStatic
  fun main(args: Array<String>) {
    val startedAtMs = System.currentTimeMillis()

    runBlocking {
      val pairs = (0 until 1000000).map { jobIndex ->
        val channel = Channel<Int>()

        async {
          if (jobIndex == 0) {
            println("hi from ${coroutineContext[Job]} $channel $jobIndex: $jobIndex")
          }

          val x = channel.receive()

          if(jobIndex == 0) {
            println("woke up!")
          }

          x * x
        } to channel
      }

      val time = 50000
      println()
      println("awaiting $time ms...")
      delay(time)

      pairs.forEachIndexed { index, (job, channel) ->
        if(index == 0) {
          println("got $job, $channel")
        }

        channel.send(index)
        job.await()
        println(".")
      }
    }
  }
}


fun main(args: Array<String>) = runBlocking<Unit> {
  val jobs = List(100_000) {
    // launch a lot of coroutines and list their jobs
    launch {
      delay(40000L)
      print(".")
    }
  }

  jobs.forEach { it.join() } // wait for all jobs to complete
}