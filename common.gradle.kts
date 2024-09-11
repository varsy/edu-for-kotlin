// Common code for settings.gradle.kts and build.gradle.kts

import org.gradle.internal.os.OperatingSystem
import java.net.InetAddress

extra["inJetBrainsNetwork"] = fun(): Boolean {
  var inJetBrainsNetwork = false
  try {
    inJetBrainsNetwork = InetAddress.getByName("repo.labs.intellij.net").isReachable(1000)
    if (!inJetBrainsNetwork && OperatingSystem.current().isWindows) {
      inJetBrainsNetwork = Runtime.getRuntime().exec("ping -n 1 repo.labs.intellij.net").waitFor() == 0
    }
  } catch (ignored: java.net.UnknownHostException) {}
  return inJetBrainsNetwork
}

extra["secretProperties"] = "secret.properties"

extra["cognifireProperties"] = "cognifire.properties"
