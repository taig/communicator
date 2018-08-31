import sbt._

object Dependencies {
    object Version {
        val circe = "0.9.3"

        val monix = "3.0.0-RC1"

        val okhttp = "3.11.0"
    }

    val circeGeneric = "io.circe" %% "circe-generic" % Version.circe

    val monixEval = "io.monix" %% "monix-eval" % Version.monix

    val okhttp = "com.squareup.okhttp3" % "okhttp" % Version.okhttp

    val okhttpMockwebserver = "com.squareup.okhttp3" % "mockwebserver" % Version.okhttp

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
}