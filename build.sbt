lazy val communicator = project.in( file( "." ) )
    .settings( tutSettings ++ Settings.common )
    .settings(
        aggregate in test := false,
        aggregate in tut := false,
        description := "An OkHttp wrapper for Scala",
        name := "Communicator",
        normalizedName := "communicator",
        releaseTagName := releaseTagName.value drop 1,
        startYear := Some( 2013 ),
        publish := (),
        publishArtifact := false,
        publishLocal := (),
        test <<= test in tests in Test,
        tut <<= tut in documentation
    )
    .aggregate( common, request, websocket )

lazy val common = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            "com.squareup.okhttp3" % "okhttp" % Settings.dependency.okhttp ::
            Nil,
        name := "Common",
        startYear := Some( 2016 )
    )

lazy val request = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            "io.monix" %% "monix-eval" % Settings.dependency.monix ::
            Nil,
        name := "Request",
        startYear := Some( 2016 )
    )
    .dependsOn( common )

lazy val websocket = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            "com.squareup.okhttp3" % "okhttp-ws" % Settings.dependency.okhttp ::
            "io.monix" %% "monix-reactive" % Settings.dependency.monix ::
            Nil,
        name := "WebSocket",
        startYear := Some( 2016 )
    )
    .dependsOn( common )

lazy val documentation = project
    .settings( tutSettings ++ Settings.common )
    .settings(
        tutTargetDirectory := file( "." )
    )
    .dependsOn( common, request, websocket )

lazy val tests = project
    .settings( Settings.common )
    .settings (
        libraryDependencies ++=
            "com.squareup.okhttp3" % "mockwebserver" % Settings.dependency.okhttp % "test" ::
            "org.scalatest" %% "scalatest" % "3.0.0-RC3" % "test" ::
            Nil
    )
    .dependsOn( common, request, websocket )