import sbt._
import sbt.Keys._

object Build extends sbt.Build
{
	val main = Project( "communicator", file( "." ) ).settings(
		name := "Communicator",
		organization := "com.taig.communicator",
		version := "0.3.3",
		autoScalaLibrary := false
	)
}
