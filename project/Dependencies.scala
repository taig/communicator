import sbt._

object Dependencies {
    object Version {
        val cats = "0.9.0"

        val monix = "2.3.3"

        val okhttp = "3.9.1"
    }

    val catsCore = "org.typelevel" %% "cats-core" % Version.cats

    val monixEval = "io.monix" %% "monix-eval" % Version.monix

    val okhttp = "com.squareup.okhttp3" % "okhttp" % Version.okhttp

    val okhttpMockwebserver = "com.squareup.okhttp3" % "mockwebserver" % Version.okhttp

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
}