package fast.dsl


data class User(
  val name: String,
  val group: String
) {

//  suspend fun apply(file: File, recursive: Boolean = true) {
//    if(this != omit) {
//      "chown ${if (recursive) "-R" else ""} $name.$group ${file.path}".exec(1000)
//    }
//  }

  companion object {
    val omit = User("omit", "")
  }
}