package fast.api

import java.io.File


data class UserRights(
  val access: String,
  val owner: User = User.omit,
  val recursive: Boolean = true,
  override val name: String = ""
) : Rights(name) {

//  override suspend fun apply(file: File) {
//    "chmod ${if (recursive) "-R" else ""} $access ${file.path}".exec(1000)
//    owner.apply(file)
//  }

  fun noRecurse() = copy(recursive = false)

  companion object {
    val omit = UserRights("", name = "omit")

    fun rights(
      access: String,
      owner: User = User.omit,
      recursive: Boolean = true,
      name: String = ""
    ) = UserRights(access, owner, recursive, name)
  }
}


sealed class Rights(open val name: String) {

  object omit : Rights("omit") {
//    override suspend fun apply(file: File) {}
  }

  open suspend fun apply(file: File) {TODO("uncommment and copy from honey mouth")}


  companion object {
    val userOnlyReadWrite = UserRights("u=rw,go=r", name = "userOnlyReadWrite")
    val userOnlyReadWriteFolder = UserRights("u=rwx,go=r", name = "userOnlyReadWrite")
    val userOnlyExecutable = UserRights("u=rwx,go=", name = "userOnlyExecutable")
    val userReadWrite = UserRights("u=rw,go=r", name = "userReadWrite")
    val writeAll = UserRights("a=rw", name = "writeAll")
    val readOnly = UserRights("a=rx", name = "readOnly")
    val executable = UserRights("u+x", name = "executable")
    val executableAll = UserRights("a+x", name = "executableAll")
    val all = UserRights("a=rwx", name = "all")
    val writeProtect = UserRights("u=rwx,go=rx", name = "writeProtect")
  }

}