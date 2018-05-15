import io.taig.sbt.sonatype.SonatypeHouserulesPlugin.autoImport._
import sbt.{Def, Tests}
import sbt.Keys._
import sbt._

object Settings {
    val Scala211 = "2.11.12"

    val Scala212 = "2.12.6"

    val common = Def.settings(
        crossScalaVersions := Scala211 :: Scala212 :: Nil,
        githubProject := "communicator",
        javacOptions ++= {
            scalaVersion.value match {
                case Scala211 =>
                    "-source" :: "1.7" ::
                    "-target" :: "1.7" ::
                    Nil
                case _ => Nil
            }
        },
        normalizedName := s"communicator-${normalizedName.value}",
        organization := "io.taig",
        scalacOptions ++=
            "-deprecation" ::
            "-feature" ::
            "-Xfatal-warnings" ::
            "-Xexperimental" ::
            "-Ywarn-dead-code" ::
            "-Ywarn-infer-any" ::
            "-Ywarn-numeric-widen" ::
            "-Ywarn-value-discard" ::
            Nil,
        scalaVersion := Scala212,
        testOptions in Test += Tests.Argument( "-oFD" )
    )
}