package fast.ssh.command.script

import fast.api.ext.ChecksumHolder

class WgetCommandDsl(
  var url: String,
  var checksum: ChecksumHolder,
  var filename: String? = url.substringAfterLast("/")
): ScriptDslSettings(), ScriptBlock {
  override fun getString(): String {
    val util = checksum.utilityName()
    return """
hashSum() {
 URL=$url
 FILE=$filename
 sha1=${checksum.get()}

 echo "${'$'}sha1 ${'$'}FILE" | $util --quiet -c -

 rc=${'$'}?

 if [ ${'$'}rc != 0 ] ; then
  rm ${'$'}FILE
  wget -O $filename ${'$'}URL
  echo "${'$'}sha1 ${'$'}FILE" | $util --quiet -c -
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