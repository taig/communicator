import sbt._

object Dependencies {
    abstract class Configuration(
        group: String,
        version: String,
        prefix: Option[String] = None
    ) {
        def this( group: String, prefix: String, version: String ) = {
            this( group, version, Some( prefix ) )
        }

        def module(
            name: String,
            f: String => ModuleID
        ): ModuleID = {
            val artifact = prefix.map( _ + "-" ).getOrElse( "" ) + name
            f( artifact )
        }

        def scala( name: String, version: String = this.version ): ModuleID = {
            module( name, group %% _ % version )
        }

        def java( name: String, version: String = this.version ): ModuleID = {
            module( name, group % _ % version )
        }
    }

    object circe extends Configuration( "io.circe", "circe", "0.6.1" ) {
        val core = scala( "core" )

        val generic = scala( "generic" )

        val parser = scala( "parser" )
    }

    object monix extends Configuration( "io.monix", "monix", "2.1.0" ) {
        val cats = scala( "cats" )

        val eval = scala( "eval" )

        val reactive = scala( "reactive" )
    }

    object okhttp extends Configuration( "com.squareup.okhttp3", "3.5.0" ) {
        val core = java( "okhttp" )

        val mockwebserver = java( "mockwebserver" )
    }

    val paradise = "org.scalamacros" % "paradise" % "2.1.0"

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"
}