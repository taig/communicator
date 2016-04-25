javacOptions ++=
    "-source" :: "1.7" ::
    "-target" :: "1.7" ::
    Nil

libraryDependencies ++=
    "com.squareup.okhttp" % "okhttp" % "2.7.4" ::
    "ch.qos.logback" % "logback-classic" % "1.1.3" % "test" ::
    "org.scalatest" %% "scalatest" % "2.2.6" % "test" ::
    "org.mock-server" % "mockserver-netty" % "3.10.2" % "test" ::
    Nil

name := "Communicator"

organization := "io.taig"

scalacOptions ++=
    "-deprecation" ::
    "-feature" ::
    Nil

scalaVersion := "2.11.8"

version := "2.2.4"