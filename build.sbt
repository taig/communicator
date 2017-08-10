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
    .aggregate( builder, request, websocket, phoenix )
    .dependsOn( builder, request, websocket, phoenix )

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

lazy val websocket = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.monixReactive ::
            Dependencies.slf4jApi ::
            Dependencies.logbackClassic % "test" ::
            Nil,
        startYear := Some( 2017 )
    )
    .dependsOn( request % "compile->compile;test->test" )

lazy val phoenix = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            Dependencies.circeParser ::
            Dependencies.phoenixModels ::
            Dependencies.monixCats % "test" ::
            Nil,
        startYear := Some( 2016 )
    )
    .dependsOn( websocket % "compile->compile;test->test" )