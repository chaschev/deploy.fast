package fast.ssh.files

import fast.ssh.*
import java.io.File

interface Files {
  val provider: ConsoleProvider

  suspend fun ls(path: String): List<RemoteFile>
  suspend fun ln(existingPath: String, linkPath: String): Boolean

  fun copyLocalFiles(dest: String, vararg files: File)
  fun writeToString(path: String, s: String)
  fun copyRemoteFiles(destDir: File, vararg sourcePaths: String): List<File>
  fun readAsString(path: String): String

  suspend fun mkdirs(vararg paths: String): Boolean
  suspend fun remove(vararg paths: String, recursive: Boolean = false): Boolean
  suspend fun chown(vararg paths: String, owner: String, recursive: Boolean = true): Boolean
  suspend fun chmod(vararg paths: String, mod: String, recursive: Boolean = true): Boolean
}

suspend fun Files.exists(path: String): Boolean {
  return ls(path.substringBeforeLast('/'))
    .find { it.name == path.substringAfterLast('/') } != null
}


