import android.Keys._
import android.Plugin._
import sbt.Keys._
import sbt._

object Build extends android.AutoBuild
{
	lazy val main = Project( "communicator", file( "." ) )
		.settings( androidBuildAar: _* )
		.settings(
			autoScalaLibrary := false,
			name := "Communicator",
			organization := "com.taig.android",
			publishArtifact in packageDoc := false,
			scalaVersion := "2.11.4",
			version := "1.0.8",
			platformTarget in Android := "android-21"
	)
}