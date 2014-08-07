import sbt._
import sbt.Keys._

object Build extends sbt.Build
{
	val main = Project( "communicator", file( "." ) ).settings(
		autoScalaLibrary := false,
		libraryDependencies += "com.google.android" % "android" % "4.4" % "provided" from ( "file://" + System.getenv( "ANDROID_HOME" ) + "/platforms/android-19/android.jar" ),
		name := "android-communicator",
		organization := "com.taig",
		version := "1.0.2"
	)
}