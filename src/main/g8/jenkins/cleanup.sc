import ammonite.ops._
import ammonite.ops.ImplicitWd._
import $exec.common, common._

def cleanImgCmd(version: String): String =
  "for imageid in $(docker images | grep \"" + version + "\" | awk '{print $3}'); do docker rmi -f $imageid; done"

val version = currentProjectVersion().getOrElse(sys.error("Project version should be provided"))
tryCmd(%('bash, "-c", cleanImgCmd(version)))(s"Cleanup of docker images is failed for version: $version")