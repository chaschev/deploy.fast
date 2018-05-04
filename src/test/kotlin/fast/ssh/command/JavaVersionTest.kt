import com.nhaarman.mockito_kotlin.mock
import fast.ssh.command.JavaVersion
import fast.ssh.command.JavaVersionCommand
import fast.ssh.command.Version
import fast.ssh.command.Version.Companion.parse
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

class JavaVersionTest {
  companion object {
    val openJDKOutput8 = """
openjdk version "1.8.0_131"
OpenJDK Runtime HiveConfig (build 1.8.0_131-8u131-b11-2ubuntu1.16.04.3-b11)
OpenJDK 64-Bit Server VM (build 25.131-b11, mixed mode)
        """.trimIndent()

    val openJDKOutput9 = """
openjdk version "9-internal"
OpenJDK Runtime HiveConfig (build 9-internal+0-2016-04-14-195246.buildd.src)
OpenJDK 64-Bit Server VM (build 9-internal+0-2016-04-14-195246.buildd.src, mixed mode)
        """.trimIndent()

    val oracleOutput = """java version "9"
Java(TM) SE Runtime HiveConfig (build 9+181)
Java HotSpot(TM) 64-Bit Server VM (build 9+181, mixed mode)
        """.trimIndent()

    val oracleJava8 = JavaVersion(false, parse("8.0"))
    val oracleJava8_131 = JavaVersion(false, parse("8.131"))

  }
  @Test
  fun testParse() {
    assertEquals(
      JavaVersion(true, parse("8.131")),
      parseJV(openJDKOutput8)
    )

    assertEquals(
      JavaVersion(true, parse("9.9")),
      parseJV(openJDKOutput9)
    )

    assertEquals(
      JavaVersion(false, parse("9.9")),
      parseJV(oracleOutput)
    )
  }

  private fun parseJV(s: String) = JavaVersionCommand(mock()).parseJavaVersion(s)

  @Test
  fun testCompareJava(){
    Assert.assertTrue(
      parseJV(openJDKOutput8).compareTo(parseJV(oracleOutput)) < 0
    )

    Assert.assertTrue(parseJV(openJDKOutput8).compareTo(oracleJava8_131) == 0)
    Assert.assertTrue(parseJV(openJDKOutput8) > oracleJava8)
    Assert.assertTrue(parseJV(openJDKOutput8) >= oracleJava8)
    Assert.assertTrue(parseJV(oracleOutput) >= oracleJava8)
    Assert.assertTrue(oracleJava8_131 >= oracleJava8)
    Assert.assertTrue(parseJV(oracleOutput) >= parseJV(openJDKOutput8))
  }

  @Test
  fun testCompareVersions() {
    Assert.assertTrue(parse("1.2") == parse("1.2"))
    Assert.assertTrue(parse("1.2") >= parse("1.2"))
    Assert.assertTrue(parse("1.2") <= parse("1.2"))
    Assert.assertTrue(parse("1") < parse("1.2"))
    Assert.assertTrue(parse("1.2.3") > parse("1.2"))
    Assert.assertTrue(parse("1.2") < parse("1.2.3"))
    Assert.assertTrue(parse("1.a") < parse("1.b"))
    Assert.assertTrue(parse("1.a") <= parse("1.a"))
    Assert.assertTrue(parse("1.a") == parse("1.a"))
  }
}
