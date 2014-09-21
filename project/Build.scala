import sbt._
import sbt.Keys._

object Build extends sbt.Build
{
	val main = Project( "communicator", file( "." ) ).settings(
		autoScalaLibrary := false,
		exportJars := true,
		libraryDependencies += "com.google.android" % "android" % "4.4" % "provided" from ( "file://" + System.getenv( "ANDROID_HOME" ) + "/platforms/android-19/android.jar" ),
		name := "communicator",
		organization := "com.taig.android",
		scalaVersion := "2.11.2",
		version := "1.0.4"
	)
}