package fast

import fast.ssh.asyncNoisy
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*

object CouroutinesTest {
  @JvmStatic
  fun main(args: Array<String>) {
    val r = Random()
    runBlocking {
      (1..100000).map {index ->
        asyncNoisy {
          //        delay(r.nextInt(2000) + 500)
          delay(1000)
          println("I am number $index, thread ${Thread.currentThread().id}")
        }
      }.forEach {
        it.await()
      }
    }
  }
}