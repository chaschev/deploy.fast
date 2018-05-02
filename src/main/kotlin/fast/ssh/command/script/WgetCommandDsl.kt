package fast.ssh.command.script

class WgetCommandDsl(
  var url: String,
  var sha1: String,
  var directory : String? = null,
  var filename: String? = TODO()
): ScriptDslSettings(), ScriptLines {
  override fun lines(): List<String> {
    return """
 URL=$url
 FILE=$filename
 sha1=$sha1

 echo "${'$'}sha1 ${'$'}FILE" | sha1sum --quiet -c -

 rc=${'$'}?

 if [ ${'$'}rc != 0 ] ; then
  rm ${'$'}FILE
  wget ${'$'}URL
  echo "${'$'}sha1 ${'$'}FILE" | sha1sum --quiet -c -
  rc=${'$'}?
 fi

 echo "wget result: ${'$'}rc"

 return ${'$'}rc
}

hashSum

echo ${'$'}?
    """.trimIndent().lines();
  }
}