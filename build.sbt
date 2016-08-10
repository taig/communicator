lazy val communicator = project.in( file( "." ) )
    .settings( tutSettings ++ Settings.common )
    .settings(
        aggregate in test := false,
        aggregate in tut := false,
        description := "An OkHttp wrapper for Scala",
        name := "communicator",
        normalizedName := name.value,
//        releaseProcess := Settings.releaseSteps,
        startYear := Some( 2013 ),
        test <<= test in tests in Test,
        tut <<= tut in documentation
    )
    .aggregate( common, request, websocket, phoenix )
    .dependsOn( common, request, websocket, phoenix )

lazy val common = project
    .settings( Settings.common )
    .settings(
        libraryDependencies ++=
            "com.squareup.okhttp3" % "okhttp" % Settings.dependency.okhttp ::
            "io.monix" %% "monix-eval" % Settings.dependency.monix ::
            Nil,
        name := "common",
        startYear := Some( 2016 )
    )

lazy val request = project
    .settings( Settings.common )
    .settings(
        name := "request",
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
        name := "websocket",
        startYear := Some( 2016 )
    )
    .dependsOn( common )

lazy val phoenix = project
    .settings( Settings.common )
    .settings(
        name := "phoenix",
        startYear := Some( 2016 )
    )
    .dependsOn( websocket )

lazy val documentation = project
    .settings( tutSettings ++ Settings.common )
    .settings(
        tutScalacOptions :=
            "-deprecation" ::
            "-feature" ::
            "-Xfatal-warnings" ::
            Nil,
        tutTargetDirectory := file( "." )
    )
    .dependsOn( common, request, websocket )

lazy val tests = project
    .settings( Settings.common )
    .settings (
        libraryDependencies ++=
            "com.squareup.okhttp3" % "mockwebserver" % Settings.dependency.okhttp % "test" ::
            "org.scalatest" %% "scalatest" % "3.0.0" % "test" ::
            Nil
    )
    .dependsOn( common, request, websocket )