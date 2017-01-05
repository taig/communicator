lazy val communicator = project.in( file( "." ) )
    .settings( tutSettings ++ Settings.common )
    .settings(
        aggregate in tut := false,
        description := "An OkHttp wrapper for Scala",
        name := "communicator",
        normalizedName := name.value,
//        releaseProcess := Settings.releaseSteps,
        startYear := Some( 2013 ),
        tutTargetDirectory := file( "." )
    )
    .aggregate( request, phoenix )
    .dependsOn( request, phoenix )

lazy val request = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.okhttp.core ::
            Dependencies.monix.eval ::
            Dependencies.okhttp.mockwebserver % "test" ::
            Dependencies.scalatest % "test" ::
            Nil,
        name := "request",
        startYear := Some( 2016 )
    )

lazy val phoenix = project
    .settings( Settings.common )
    .settings(
        addCompilerPlugin( Dependencies.paradise cross CrossVersion.full ),
        libraryDependencies ++=
            Dependencies.circe.core ::
            Dependencies.circe.generic ::
            Dependencies.circe.parser ::
            Dependencies.monix.reactive ::
            Dependencies.slf4j.api ::
            Dependencies.logback.classic % "test" ::
            Dependencies.monix.cats % "test" ::
            Nil,
        startYear := Some( 2016 )
    )
    .dependsOn( request % "compile->compile;test->test" )