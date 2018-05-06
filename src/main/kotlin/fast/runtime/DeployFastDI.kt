package fast.runtime

import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

object DeployFastDI {
  var FAST = Kodein {
    bind<AppContext>() with singleton { AppContext() }
  }
    set(value) {
      FASTD = value.direct
      field = value
    }

  var FASTD = FAST.direct
}