//addSbtPlugin( "com.github.gseitz" % "sbt-release" % "1.0.3" )

addSbtPlugin( "com.typesafe.sbt" % "sbt-git" % "0.8.5" )

addSbtPlugin( "io.taig" % "sbt-scalariform" % "1.7.1" )

addSbtPlugin( "io.taig" % "sbt-sonatype" % "1.1.0" )

addSbtPlugin( "org.scoverage" % "sbt-scoverage" % "1.4.0" )

addSbtPlugin( "org.tpolecat" % "tut-plugin" % "0.4.7" )

libraryDependencies ++=
    "ch.qos.logback" %  "logback-classic" % "1.1.7" ::
    Nil