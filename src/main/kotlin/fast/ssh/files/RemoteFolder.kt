package fast.ssh.files

interface RemoteFolder : RemoteFile {
    fun getBabies(): List<RemoteFile>
}