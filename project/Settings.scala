import io.taig.sbt.sonatype.Plugin.autoImport._
import sbt.Def
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import tut.Plugin._

object Settings {
    val common = Def.settings(
        githubProject := "communicator",
        javacOptions ++=
            "-source" :: "1.7" ::
            "-target" :: "1.7" ::
            Nil,
        normalizedName := s"communicator-${normalizedName.value}",
        organization := "io.taig",
        releaseTagName := releaseTagName.value drop 1,
        scalacOptions ++=
            "-deprecation" ::
            "-feature" ::
            "-Xfatal-warnings" ::
            "-Ywarn-dead-code" ::
            "-Ywarn-infer-any" ::
            "-Ywarn-numeric-widen" ::
            "-Ywarn-unused-import" ::
            "-Ywarn-value-discard" ::
            Nil,
        scalaVersion := "2.11.8"
    )
    
    val releaseSteps: Seq[ReleaseStep] = Seq(
//        checkSnapshotDependencies,
//        Release.inquireVersions,
//        runTest,
        Release.updateChangelog//,
//        releaseStepTaskAggregated( tut ),
//        setReleaseVersion,
//        Release.commitReleaseVersion,
//        Release.tagRelease,
//        publishArtifacts,
//        Release.setNextVersion,
//        Release.commitNextVersion,
//        Release.pushChanges
    )

    object dependency {
        val monix = "2.0-RC8"

        val okhttp = "3.3.1"
    }
}