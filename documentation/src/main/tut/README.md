# Communicator

[![CircleCI](https://circleci.com/gh/Taig/communicator/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/communicator/tree/master)
[![codecov](https://codecov.io/github/Taig/Communicator/coverage.svg?branch=master)](https://codecov.io/github/Taig/Communicator?branch=master)
[![Maven](https://img.shields.io/maven-central/v/io.taig/communicator_2.11.svg)](http://search.maven.org/#artifactdetails%7Cio.taig%7Ccommunicator_2.11%7C3.0.0%7Cjar)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/Taig/Communicator/master/LICENSE)

> A [monix][1] wrapper for [OkHttp][2]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Installation

```scala
libraryDependencies ++=
    "io.taig" %% "communicator-common" % "3.0.0-SNAPSHOT" ::
    "io.taig" %% "communicator-builder" % "3.0.0-SNAPSHOT" ::
    "io.taig" %% "communicator-request" % "3.0.0-SNAPSHOT" ::
    "io.taig" %% "communicator-websocket-experimental" % "3.0.0-SNAPSHOT" ::
    "io.taig" %% "communicator-phoenix-experimental" % "3.0.0-SNAPSHOT" ::
    Nil
```

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.0.0-SNAPSHOT"
```

## Quickstart

```tut
import io.taig.communicator._; import request._; import monix.eval.Task; import okhttp3.OkHttpClient

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = new OkHttpClient()

// Simple OkHttp request builder
val builder = new OkHttpRequest.Builder().url( "http://taig.io/" )

// Construct a Task[Response]
val request: Request = Request( builder.build() )

// Parse the response to a String
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

### Websockets

Lorem Ipsum

### Phoenix Channels

Lorem Ipsum

## Testing

To run the Phoenix-module specific tests, the [phoenix_echo][5] app (thanks [@PragTob][6]) has to be running in the background. The easiest way to do so is via the included `docker` configuration.
```
docker pull taig/communicator:latest
docker build -t taig/communicator:latest .
docker -v "$PWD:/communicator/" --entrypoint="./test.sh" taig/communicator:latest
```

## Communicator 2.x

The `scala.concurrent.Future` predecessor of this library has been deprecated. You can still [access][3] the source and documentation.

## Communicator 1.x

The Java predecessor of this library has been deprecated. You can still [access][4] the source and documentation.

[1]: https://monix.io/
[2]: http://square.github.io/okhttp/
[3]: https://github.com/Taig/Communicator/tree/2.3.2
[4]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693
[5]: https://github.com/PragTob/phoenix_echo
[6]: https://github.com/PragTob