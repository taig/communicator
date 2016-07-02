# Communicator

[![CircleCI](https://circleci.com/gh/Taig/Communicator/tree/develop-websocket.svg?style=shield)](https://circleci.com/gh/Taig/Communicator/tree/develop-websocket)
[![codecov](https://codecov.io/github/Taig/Communicator/coverage.svg?branch=develop-websocket)](https://codecov.io/github/Taig/Communicator?branch=develop-websocket)
[![Maven](https://img.shields.io/maven-central/v/io.taig/communicator_2.11.svg)](http://search.maven.org/#artifactdetails%7Cio.taig%7Ccommunicator_2.11%7C3.0.0%7Cjar)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/Taig/Communicator/master/LICENSE)

> A [`monix.Task`][1] wrapper for [OkHttp][2]

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
libraryDependencies += "io.taig" %% "communicator-request" % "3.0.0-SNAPSHOT"
libraryDependencies += "io.taig" %% "communicator-websocket" % "3.0.0-SNAPSHOT"
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

## Usage

Lorem Ipsum

### Building Requests

Lorem Ipsum

### Parsing Content

Lorem Ipsum

## Roadmap

 * Easy API to track progress updates in Requests and Responses
 * Additional module for OkHttp WebSocket
 * Additional module for Phoenix WebSocket

## Communicator 2.x

The `scala.concurrent.Future` predecessor of this library has been deprecated. You can still [access][3] the source and documentation.

## Communicator 1.x

The Java predecessor of this library has been deprecated. You can still [access][4] the source and documentation.

## License

The MIT License (MIT)  
Copyright (c) 2016 Niklas Klein <mail@taig.io>

[1]: https://monix.io/
[2]: http://square.github.io/okhttp/
[3]: https://github.com/Taig/Communicator/tree/2.3.2
[4]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693
