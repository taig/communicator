import sbt._

object Dependencies {
    abstract class Configuration(
        group: String,
        version: String,
        prefix: Option[String] = None
    ) {
        def this( group: String, prefix: String, version: String ) =
            this( group, version, Some( prefix ) )

        def module(
            name: String,
            f: String => ModuleID
        ): ModuleID = {
            val artifact = prefix.map( _ + "-" ).getOrElse( "" ) + name
            f( artifact )
        }

        def scala( name: String, version: String = this.version ): ModuleID =
            module( name, group %% _ % version )

        def java( name: String, version: String = this.version ): ModuleID =
            module( name, group % _ % version )
    }

    object cats extends Configuration( "org.typelevel", "cats", "0.9.0" ) {
        val core = scala( "core" )

        val kernel = scala( "kernel" )

        val macros = scala( "macros" )
    }

    object circe extends Configuration( "io.circe", "circe", "0.7.0" ) {
        val parser = scala( "parser" )
    }

    object logback extends Configuration( "ch.qos.logback", "logback", "1.1.9" ) {
        val classic = java( "classic" )
    }

    object monix extends Configuration( "io.monix", "monix", "2.1.2" ) {
        val cats = scala( "cats" )

        val eval = scala( "eval" )

        val reactive = scala( "reactive" )
    }

    object okhttp extends Configuration( "com.squareup.okhttp3", "3.5.0" ) {
        val core = java( "okhttp" )

        val mockwebserver = java( "mockwebserver" )
    }

    val phoenixModels = "io.taig" %% "phoenix-models" % "1.0.1"

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"

    object slf4j extends Configuration( "org.slf4j", "slf4j", "1.7.22" ) {
        val api = java( "api" )
    }
}