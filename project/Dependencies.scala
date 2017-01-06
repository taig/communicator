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
        val parser = scala( "parser" )
    }

    object logback extends Configuration( "ch.qos.logback", "logback", "1.1.8" ) {
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

    val phoenixModels = "io.taig" %% "phoenix-models" % "1.0.0"

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"

    object slf4j extends Configuration( "org.slf4j", "slf4j", "1.7.22" ) {
        val api = java( "api" )
    }
}