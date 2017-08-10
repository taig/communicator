import sbt._

object Dependencies {
    object Version {
        val cats = "0.9.0"

        val monix = "2.3.0"

        val okhttp = "3.8.1"
    }

    val catsCore = "org.typelevel" %% "cats-core" % Version.cats

    val catsKernel = "org.typelevel" %% "cats-kernel" % Version.cats

    val catsMacros = "org.typelevel" %% "cats-macros" % Version.cats

    val circeParser = "io.circe" %% "circe-parser" % "0.8.0"

    val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

    val monixCats = "io.monix" %% "monix-cats" % Version.monix

    val monixEval = "io.monix" %% "monix-eval" % Version.monix

    val monixReactive = "io.monix" %% "monix-reactive" % Version.monix

    val okhttp = "com.squareup.okhttp3" % "okhttp" % Version.okhttp

    val okhttpMockwebserver = "com.squareup.okhttp3" % "mockwebserver" % Version.okhttp

    val phoenixModels = "io.taig" %% "phoenix-models" % "1.0.2"

    val scalatest = "org.scalatest" %% "scalatest" % "3.0.3"

    val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
}