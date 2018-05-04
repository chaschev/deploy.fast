package honey.util.command

import fast.ssh.command.Regexes
import fast.ssh.command.getLineWithMe
import org.junit.Assert
import org.junit.Test

class RegexesTest {
    @Test
    fun getERRORS() {
        val s = """openjdk version "1.8.0_131"
OpenJDK Runtime HiveConfig (build 1.8.0_131-8u131-b11-2ubuntu1.16.04.3-b11)
OpenJDK error! 64-Bit Server VM (build 25.131-b11, mixed mode)
10:05:03.144 [-worker-1] INFO  honey.util.ConsoleProcess - reached end: ConsoleCommandResult(console=Console(result=null, stdout=), exitCode=0, isEOF=true, isTimeout=false, timeMs=104) for command ````java -version` @stage2```
java version: can't! openjdk version "1.8.0_131"
OpenJDK Runtime HiveConfig (build 1.8.0_131-8u131-b11-2ubuntu1.16.04.3-b11)
OpenJDK 64-Bit Server VM (build 25.131-b11, mixed mode)
        """

        val errors = Regexes.ERRORS.findAll(s.trimIndent()).map { it.groups[0]!!.getLineWithMe(s) }.toList()

        val lines = s.split('\n')

        println(errors.joinToString("\n"))

        Assert.assertEquals(lines[2], errors[0])
        Assert.assertEquals(lines[4], errors[1])

    }

    @Test
    fun getLine(){
        val regex = "abc".toRegex()

        checkGetLine("abc", regex)
        checkGetLine("abc\n", regex)
        checkGetLine("\nabc", regex)
        checkGetLine("\nabc\n", regex)
        checkGetLine("\nyyabcyy\n", regex)
    }

    private fun checkGetLine(line: String, regex: Regex) {
        Assert.assertEquals(line.trim(),
            regex.findAll(line).map { it.groups[0]!!.getLineWithMe(line) }.first())
    }
}

