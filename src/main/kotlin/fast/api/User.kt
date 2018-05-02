package fast.api


data class User(
  val name: String,
  val group: String? = name,
  val password: String? = null
) {

//  suspend fun apply(file: File, recursive: Boolean = true) {
//    if(this != omit) {
//      "chown ${if (recursive) "-R" else ""} $name.$group ${file.path}".exec(1000)
//    }
//  }

  fun omit(): Boolean = name == "omit"

  companion object {
    val omit = User("omit")
  }
}

