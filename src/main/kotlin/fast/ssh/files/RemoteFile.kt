package fast.ssh.files

interface RemoteFile {
    val name: String
    val isFolder: Boolean
    val group: String?
    val user: String?
    val size: Long
    val path: String
    val unixRights: String
    val lastModified: String
}

data class RemoteFileImpl(override val name: String, override val isFolder: Boolean, override val group: String?, override val user: String?, override val size: Long, override val path: String, override val unixRights: String, override val lastModified: String) : RemoteFile