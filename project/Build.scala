import sbt._
import sbt.Keys._

object	Build
extends	sbt.Build
{
	lazy val main = Project( "communicator", file( "." ) )
		.settings(
			libraryDependencies ++= Seq(
				"com.squareup.okhttp" % "okhttp" % "2.2.0",
				"org.scalatest" %% "scalatest" % "2.2.4" % "test",
				"org.mock-server" % "mockserver-netty" % "3.9.1" % "test"
			),
			organization := "io.taig",
			scalacOptions ++= Seq( "-deprecation", "-feature" ),
			scalaVersion := "2.11.5"
		)
}