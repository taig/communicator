addSbtPlugin( "com.lucidchart" % "sbt-scalafmt" % "1.15" )

addSbtPlugin( "com.typesafe.sbt" % "sbt-git" % "0.9.3" )

addSbtPlugin( "io.taig" % "sbt-sonatype-houserules" % "1.2.0" )

addSbtPlugin( "org.scoverage" % "sbt-scoverage" % "1.5.1" )

addSbtPlugin( "org.tpolecat" % "tut-plugin" % "0.6.7" )

libraryDependencies ++=
    "ch.qos.logback" %  "logback-classic" % "1.2.3" ::
    Nil