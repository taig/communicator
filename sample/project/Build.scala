import sbt._
import sbt.Keys._
import AndroidKeys._

object Settings
{
	val default = Defaults.defaultSettings ++ Seq(
		name := "CommunicatorSample",
		version := "0.1",
		versionCode := 0,
		scalaVersion := "2.10.2",
		platformName in Android := "android-17",
		scalacOptions ++= Seq( "-feature", "-language:implicitConversions" ),
		javacOptions ++= Seq( "-encoding", "UTF-8", "-source", "1.6", "-target", "1.6" )
	)

	val proguard = Seq( useProguard in Android := false )

	lazy val android =
		Settings.default ++
		AndroidProject.androidSettings ++
		TypedResources.settings ++
		proguard ++
		AndroidManifestGenerator.settings ++
		AndroidMarketPublish.settings ++
		Seq( keyalias in Android := "change-me" )
}

object AndroidBuild extends Build
{
	val communicator = ProjectRef( file( "../" ), "communicator" )

	val main = Project( "communicator-sample", file( "." ), settings = Settings.android )
			.settings( libraryDependencies += "org.jsoup" % "jsoup" % "1.7.2" )
			.dependsOn( communicator )
			.aggregate( communicator )
}