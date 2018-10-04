import ammonite.ops.ImplicitWd._
import ammonite.ops._

import scala.util.Try

def branchName: Option[String] = sys.env.get("BRANCH_NAME")

def buildNumber: Option[Int] = sys.env.get("BUILD_NUMBER").map(_.toInt)

def fail(msg: String)(t: Throwable): Nothing = sys.error(s"$msg with error: $t")

def tryCmd[A](cmd: => A)(errorMsg: String): Unit = Try(cmd).failed.foreach(fail(errorMsg))

def projectVersion(): Option[String] = {
  val command = "git describe --abbrev=0 --tags"
  %%('bash, "-c", command).out.lines.headOption
}

def projectVersionBranch(): Option[String] =
  sbtVersion().map(_.replaceAll("-", "_")).map(_ + "_" + branchNameExtension)

def sbtVersion(): Option[String] = {
  def extractVersion(s: String) =
    s.splitAt(s.indexOf("=") + 1)._2.replaceAll("\"", "").trim

  val line = (read.lines! pwd/"version.sbt").headOption
  line.map(extractVersion)
}

def currentProjectVersion(): Option[String] = {
  if (branchName.contains("master")) {
    projectVersion()
  } else {
    projectVersionBranch()
  }
}

def branchNameExtension: String = {
  val branchName = cleanBranchName

  if (runningOnJenkins)
    s"${branchName}_${buildNumber.getOrElse("")}"
  else
    branchName
}

def runningOnJenkins: Boolean =
  branchName.isDefined && buildNumber.isDefined

def cleanBranchName: String =
  branchName.map(normalizeToAcceptableNamespace).getOrElse("na")

def normalizeToAcceptableNamespace(name: String): String = {
  def dropLeadingNonAlphaNumeric(s: String) = {
    s.dropWhile(!_.isLetterOrDigit)
  }

  val alphaNumOrDash = name
    .take(16)
    .map(c => if (c.isLetterOrDigit) c.toLower else '-')

  dropLeadingNonAlphaNumeric(dropLeadingNonAlphaNumeric(alphaNumOrDash).reverse).reverse
}