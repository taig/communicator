# Communicator

[![Circle CI](https://circleci.com/gh/Taig/Communicator.svg?style=shield)](https://circleci.com/gh/Taig/Communicator)
[![codecov](https://codecov.io/gh/Taig/Communicator/branch/develop-0.3.0/graph/badge.svg)](https://codecov.io/gh/Taig/Communicator)

> A [`monix.Task`][3] wrapper for [OkHttp][1]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Index

1. [Installation](#installation)
2. [Quickstart](#quickstart)
3. [Usage](#usage)
    1. [Building Requests](#building-requests)
    2. [Parsing Content](#parsing-content)
4. [Roadmap](#roadmap)
5. [Communicator 2.x](#communicator-2x)
6. [Communicator 1.x](#communicator-1x)
7. [License](#license)

## Installation

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.0.0-SNAPSHOT"
```

## Quickstart

```tut
import io.taig.communicator._

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = Client()

// Simple OkHttp request builder
val builder = Request.Builder().url( "http://taig.io/" )

// Construct a Task[Response] and parse the content to a String
import monix.eval.Task

val request: Request = Request( builder.build() )

// Parse the content to a String
val requestContent: Task[Response.With[String]] = request.parse[String]

// Kick off the actual request
import monix.execution.Scheduler.Implicits.global
import scala.util.{ Failure, Success }

requestContent.runAsync.andThen {
    case Success( content ) => "Success"
    case Failure( exception ) => "Failure"
}
```

## Communicator 2.x

The `scala.concurrent.Future` predecessor of this library has been deprecated. You You can still [access][3] the source and documentation.

## Communicator 1.x

The Java predecessor of this library has been deprecated. You can still [access][4] the source and documentation.

## License

The MIT License (MIT)  
Copyright (c) 2016 Niklas Klein <mail@taig.io>

[1]: http://square.github.io/okhttp/
[2]: https://monix.io/
[3]: ???
[4]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693
