import sbt._
import sbt.Keys._

object Build extends sbt.Build
{
	val main = Project( "communicator", file( "." ) ).settings(
		name := "Communicator",
		organization := "com.taig.communicator",
		version := "0.6.2",
		autoScalaLibrary := false,
		libraryDependencies += "com.google.android" % "android" % "4.3" % "provided" from ( "file://" + System.getenv( "ANDROID_HOME" ) + "/platforms/android-18/android.jar" )
	)
}