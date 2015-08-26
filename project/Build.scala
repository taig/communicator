import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.SonatypeKeys._

object	Build
extends	sbt.Build
{
	val main = Project( "communicator", file( "." ), settings = sonatypeSettings )
		.settings(
			description := "An OkHttp wrapper for Scala built with Android in mind",
			homepage := Some( url( "https://github.com/taig/communicator" ) ),
			licenses := Seq( "MIT" -> url( "https://raw.githubusercontent.com/taig/communicator/master/LICENSE" ) ),
			organizationHomepage := Some( url( "http://taig.io" ) ),
			pomExtra :=
			{
				<issueManagement>
					<url>https://github.com/taig/communicator/issues</url>
					<system>GitHub Issues</system>
				</issueManagement>
				<developers>
					<developer>
						<id>Taig</id>
						<name>Niklas Klein</name>
						<email>mail@taig.io</email>
						<url>http://taig.io/</url>
					</developer>
				</developers>
			},
			pomIncludeRepository := { _ => false },
			publishArtifact in Test := false,
			publishMavenStyle := true,
			publishTo <<= version ( version =>
			{
				val url = Some( "https://oss.sonatype.org/" )

				if( version.endsWith( "SNAPSHOT" ) )
				{
					url.map( "snapshot" at _ + "content/repositories/snapshots" )
				}
				else
				{
					url.map( "release" at _ + "service/local/staging/deploy/maven2" )
				}
			} ),
			scmInfo := Some(
				ScmInfo(
					url( "https://github.com/taig/communicator" ),
					"scm:git:git://github.com/taig/communicator.git",
					Some( "scm:git:git@github.com:taig/communicator.git" )
				)
			),
			startYear := Some( 2013 )
		)
}