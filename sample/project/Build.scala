import sbt._
import sbt.Keys._
import sbtandroid.AndroidDefaults
import sbtandroid.AndroidPlugin._

object Build extends sbt.Build
{
	val communicator = ProjectRef( file( "../" ), "communicator" )

	val main = Project( "communicator-sample", file( "." ), settings = androidDefaults )
			.settings(
				name := "CommunicatorSample",
				version := "0.1",
				versionCode := 0,
				scalaVersion := "2.10.2",
				platformName := "android-17",
				libraryDependencies += "org.jsoup" % "jsoup" % "1.7.2" )
			.dependsOn( communicator )
			.aggregate( communicator )
}