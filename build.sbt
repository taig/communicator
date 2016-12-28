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
            "com.squareup.okhttp3" % "okhttp" % Settings.dependency.okhttp ::
            "io.monix" %% "monix-eval" % Settings.dependency.monix ::
            "com.squareup.okhttp3" % "mockwebserver" % Settings.dependency.okhttp % "test" ::
            "org.scalatest" %% "scalatest" % "3.0.1" % "test" ::
            Nil,
        name := "request",
        startYear := Some( 2016 )
    )

lazy val phoenix = project
    .settings( Settings.common )
    .settings(
        addCompilerPlugin( "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full ),
        libraryDependencies ++=
            "io.circe" %% "circe-core" % Settings.dependency.circe ::
            "io.circe" %% "circe-generic" % Settings.dependency.circe ::
            "io.circe" %% "circe-parser" % Settings.dependency.circe ::
            "io.monix" %% "monix-reactive" % Settings.dependency.monix ::
            "io.monix" %% "monix-cats" % Settings.dependency.monix % "test" ::
            Nil,
        startYear := Some( 2016 )
    )
    .dependsOn( request % "compile->compile;test->test" )