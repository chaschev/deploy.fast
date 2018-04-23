package fast.dsl

import java.io.File


data class UserRights(
  override val name: String,
  val access: String,
  val owner: User = User.omit,
  val recursive: Boolean = true
) : Rights(name) {

//  override suspend fun apply(file: File) {
//    "chmod ${if (recursive) "-R" else ""} $access ${file.path}".exec(1000)
//    owner.apply(file)
//  }

  fun noRecurse() = copy(recursive = false)

  companion object {
    val omit = UserRights("omit", "")
  }
}


sealed class Rights(open val name: String) {

  object omit : Rights("omit") {
//    override suspend fun apply(file: File) {}
  }

  open suspend fun apply(file: File) {TODO("uncommment and copy from honey mouth")}


  companion object {
    val readOnly = UserRights("readOnly", "a=rx")
    val executable = UserRights("executable", "u+x")
    val executableAll = UserRights("executable", "a+x")
    val all = UserRights("all", "a=rwx")
    val writeProtect = UserRights("writeProtect", "u=rwx,go=rx")
  }

}