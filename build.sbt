lazy val communicator = project.in( file( "." ) )
    .settings( tutSettings ++ Settings.common )
    .settings(
        aggregate in tut := false,
        autoScalaLibrary := false,
        description := "An OkHttp wrapper for Scala",
        name := "communicator",
        managedSources := Seq.empty,
        normalizedName := name.value,
        pomAllRepositories := false,
        pomIncludeRepository := { _ => false },
        publishArtifact in Compile := false,
        publishMavenStyle := true,
//        releaseProcess := Settings.releaseSteps,
        sources in Compile := Seq.empty,
        startYear := Some( 2013 ),
        tutTargetDirectory := file( "." )
    )
    .aggregate( request, builder, phoenix )
    .dependsOn( request, builder, phoenix )

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

lazy val builder = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.cats.core ::
            Nil,
        name := "builder",
        startYear := Some( 2016 )
    )
    .dependsOn( request )

lazy val phoenix = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.circe.parser ::
            Dependencies.monix.reactive ::
            Dependencies.phoenixModels ::
            Dependencies.slf4j.api ::
            Dependencies.logback.classic % "test" ::
            Dependencies.monix.cats % "test" ::
            Nil,
        startYear := Some( 2016 )
    )
    .dependsOn( request % "compile->compile;test->test" )