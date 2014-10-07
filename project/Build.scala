import sbt._
import sbt.Keys._
import android.Keys._

object Build extends android.AutoBuild
{
	lazy val main = Project( "communicator", file( "." ) )
		.settings(
			autoScalaLibrary := false,
			exportJars := true,
			name := "Communicator",
			organization := "com.taig.android",
			scalaVersion := "2.11.2",
			version := "1.0.6",
			minSdkVersion in Android := "10",
			targetSdkVersion in Android := "19"
		)
}