import io.taig.sbt.sonatype.SonatypeHouserulePlugin.autoImport._
import sbt.{Def, Tests}
import sbt.Keys._
import sbt._
//import sbtrelease.ReleasePlugin.autoImport.ReleaseStep._
//import sbtrelease.ReleasePlugin.autoImport._
//import sbtrelease.ReleaseStateTransformations._
import tut.Plugin._

object Settings {
    val Scala211 = "2.11.8"
    
    val Scala212 = "2.12.1"
    
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
//        releaseTagName := releaseTagName.value drop 1,
        scalacOptions ++=
            "-deprecation" ::
            "-feature" ::
//            "-Xfatal-warnings" ::
//            "-Ywarn-dead-code" ::
//            "-Ywarn-infer-any" ::
//            "-Ywarn-numeric-widen" ::
//            "-Ywarn-unused-import" ::
//            "-Ywarn-value-discard" ::
            Nil,
        scalacOptions ++= {
            scalaVersion.value match {
                case Scala211 =>
                    "-target:jvm-1.7" ::
                    Nil
                case _ => Nil
            }
        },
        scalaVersion := Scala212,
        testOptions in Test += Tests.Argument( "-oFD" )
    )

//    val releaseSteps: Seq[ReleaseStep] = Seq(
//        checkSnapshotDependencies,
//        Release.inquireVersions,
//        runTest,
//        Release.updateChangelog,
//        releaseStepTaskAggregated( tut ),
//        setReleaseVersion,
//        Release.commitReleaseVersion,
//        Release.tagRelease,
//        publishArtifacts,
//        Release.setNextVersion,
//        Release.commitNextVersion,
//        Release.pushChanges
//    )
}