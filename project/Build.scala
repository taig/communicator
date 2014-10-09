import android.Keys._
import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtGit.git._
import com.typesafe.sbt.SbtSite.site
import sbt.Keys._
import sbt._

object Build extends android.AutoBuild
{
	lazy val main = Project( "communicator", file( "." ) )
		.settings( site.settings ++ ghpages.settings: _* )
		.settings(
			autoScalaLibrary := false,
			exportJars := true,
			name := "Communicator",
			organization := "com.taig.android",
			remoteRepo := "https://github.com/Taig/Communicator.git",
			scalaVersion := "2.11.2",
			version := "1.0.7",
			minSdkVersion in Android := "10",
			targetSdkVersion in Android := "19",
			typedResources in Android := false
		)
}