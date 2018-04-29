package fast.api

data class Users(
  val default: User,
  val list: ArrayList<User>
) {
  operator fun get(name: String): User? = list.find { it.name == name }
}