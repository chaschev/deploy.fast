package fast.api.ext

import java.io.File

/*
https://www.freedesktop.org/software/systemd/man/systemd.service.html
https://www.freedesktop.org/software/systemd/man/systemd.exec.html
 */

class SystemdTemplate(
  val config: SystemdConfig
) {
  fun generate() = configureToString(config)

  companion object {
    fun configureToString(config: SystemdConfig): String =
      with(config) {

        val userSection = if(user != null) "User=$user" else ""

        val pidfileSection = if(pidfile != null) "PIDFile=$pidfile" else ""

        val envString = env.entries.joinToString(" ") { (k,v) -> "\"$k=$v\"" }

        return """
# Managed by Deploy.Fast

[Unit]
Description=$description

[Service]
$userSection
$pidfileSection
ExecStart=$exec
Restart=on-failure
Environment=$envString
WorkingDirectory=$directory

[Install]
WantedBy=multi-user.target
""".trim()
      }
  }

}