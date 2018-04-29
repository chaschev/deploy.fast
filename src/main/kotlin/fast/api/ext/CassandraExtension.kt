package fast.api.ext

/*
class CassandraExtension(): DeployFastExtension() {
  val zippedApp = ZippedAppExtension()

  override val tasks: NamedExtTasks
    get() = TODO("not implemented")

  companion object {
    fun dsl() = DeployFastDSL.deployFast(CassandraExtension()) {
      //      autoInstall()

      info {
        name = "Cassandra Extension"
        author = "Andrey Chaschev"
      }

      beforePlay {
        init {
          ext.zippedApp.configure("cassandra", "3.1.12", "TODO") {
            archiveName = "$name-$version.tar.gz"

            symlinks {
              "cassandra" to "/bin/cassandra"
            }
          }

          ext.zippedApp.tasks.install().after.append {
            // TODO install as a service
            // TODO run
            TaskResult()
          }
        }
        play {
          task("update_conf") {
            //TODO(process template)
            TaskResult()
          }

          task("install") {
            ext.zippedApp.tasks.install().run()
          }

          task("install service") {
            //            ext.zippedApp.tasks.installService().run()
            TODO()
          }
        }

        afterPlay {
          task("check_install") {
            //            ext.zippedApp.tasks.getServiceState(installed = true, running = true).run()
            TODO()
          }
        }
      }



      play {
        task("install_cassandra") {
          ext.zippedApp.tasks.install().run()
        }

//      task("create_symlinks"){
//        ext.zippedApp
//      }
      }

    }

  }
}
*/


