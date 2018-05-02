package fast.ssh.command.script

class WgetCommandDsl(
  var url: String,
  var sha1: String,
  var filename: String? = url.substringAfterLast("/")
): ScriptDslSettings(), ScriptBlock {
  override fun getString(): String {
    return """
hashSum() {
 URL=$url
 FILE=$filename
 sha1=$sha1

 echo "${'$'}sha1 ${'$'}FILE" | sha1sum --quiet -c -

 rc=${'$'}?

 if [ ${'$'}rc != 0 ] ; then
  rm ${'$'}FILE
  wget -O $filename ${'$'}URL
  echo "${'$'}sha1 ${'$'}FILE" | sha1sum --quiet -c -
  rc=${'$'}?
 fi

 echo "wget result: ${'$'}rc"

 return ${'$'}rc
}

hashSum

echo ${'$'}?
    """
  }
}