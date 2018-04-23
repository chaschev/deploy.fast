package fast.dsl


class GradleExtension(
): DeployFastExtension() {
  val zippedApp = ZippedAppExtension()

  companion object {
    fun dsl() = DeployFastDSL.deployFast(GradleExtension()) {
      info {
        name = "Gradle Extension"
        author = "Andrey Chaschev"
      }

      beforePlay {
        init {
          ext.zippedApp.configure("gradle", "4.3.2", "TODO") {
            archiveName = "$name-$version.tar.gz"

            symlinks {
              "gradle" to "/bin/gradle.sh" with UserRights.omit
            }
          }
        }
      }

      play {
        task("install_gradle") {
          ext.zippedApp.tasks.install().run()
        }
      }
    }
  }
}


