lazy val root = project.in( file( "." ) )
    .enablePlugins( TutPlugin )
    .settings( Settings.common )
    .settings(
        aggregate in tut := false,
        autoScalaLibrary := false,
        description := "A monix wrapper for OkHttp",
        fork in tut := true,
        name := "communicator",
        managedSources := Seq.empty,
        normalizedName := name.value,
        pomAllRepositories := false,
        pomIncludeRepository := { _ => false },
        publishArtifact in Compile := false,
        publishMavenStyle := true,
        sources in Compile := Seq.empty,
        startYear := Some( 2013 ),
        tutTargetDirectory := file( "." )
    )
    .aggregate( builder, request )
    .dependsOn( builder, request )

lazy val builder = project
    .settings( Settings.common )
    .settings(
        coverageEnabled := false,
        libraryDependencies ++=
            Dependencies.catsCore ::
            Nil,
        name := "builder",
        startYear := Some( 2016 )
    )
    .dependsOn( request )

lazy val request = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.okhttp ::
            Dependencies.monixEval ::
            Dependencies.okhttpMockwebserver % "test" ::
            Dependencies.scalatest % "test" ::
            Nil,
        name := "request",
        startYear := Some( 2016 )
    )