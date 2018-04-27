package fast.runtime

import fast.dsl.DeployFastApp
import fast.dsl.DeployFastAppDSL
import fast.dsl.DeployFastDSL
import fast.dsl.TaskResult.Companion.ok
import fast.dsl.ext.OpenJdkConfig
import fast.dsl.ext.OpenJdkExtension
import fast.dsl.ext.VagrantConfig
import fast.dsl.ext.VagrantExtension
import org.kodein.di.generic.instance



 fun String.env() = System.getenv(this)