import ammonite.ops.%
import ammonite.ops.ImplicitWd._
import $exec.common, common._

def setNextSnapshotVersion(): Unit =
  tryCmd(%('bash, "-c", "RELEASE_PUBLISH=true sbt 'release with-defaults'"))(s"sbt release failed")

def gitCheckoutAndSbtRelease(): Unit = {
  tryCmd(%('git, "checkout", branchName))(s"Failed to switch to $branchName branch")
  tryCmd(%('bash, "-c", "RELEASE_VERSION_BUMP=true sbt 'release with-defaults'"))(s"sbt release failed")
}