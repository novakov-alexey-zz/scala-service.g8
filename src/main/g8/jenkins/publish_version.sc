import ammonite.ops.ImplicitWd._
import ammonite.ops._
import $exec.common, common._
import $file.sbt_common, sbt_common._

import scala.util.Try

def gitPush(): Unit = {
  println("\nGoing to push new sbt version to Git")
  val errorMsg = for {
    branch <- branchName.toRight(new RuntimeException("branch name should not be empty"))

    command = s"git push origin $branch --follow-tags --verbose"
    _ = println(s"Executing: $command")
    _ <- Try(%('bash, "-c", command)).toEither

  } yield ()

  errorMsg.fold(fail(s"git push failed"), identity)
  println("\nPushed new sbt version to Git")
}

setNextSnapshotVersion()
gitPush()
