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
    "io.taig" %% "communicator-common" % "3.0.0-RC3" ::
    "io.taig" %% "communicator-request" % "3.0.0-RC3" ::
    "io.taig" %% "communicator-websocket" % "3.0.0-RC3" ::
    "io.taig" %% "communicator-phoenix" % "3.0.0-RC3" ::
    Nil
```

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.0.0-RC3"
```

## Quickstart

```scala
scala> import io.taig.communicator._; import request._; import monix.eval.Task
import io.taig.communicator._
import request._
import monix.eval.Task

scala> // To build request tasks, an implicit OkHttpClient should be in scope
     | implicit val client = Client()
client: io.taig.communicator.Client = okhttp3.OkHttpClient@702e1c9c

scala> // Simple OkHttp request builder
     | val builder = Request.Builder().url( "http://taig.io/" )
builder: okhttp3.Request.Builder = okhttp3.Request$Builder@6ec199a0

scala> // Construct a Task[Response]
     | val request: Request = Request( builder.build() )
request: io.taig.communicator.request.Request = io.taig.communicator.request.Request@72a93c0b

scala> // Parse the response to a String
     | val requestContent: Task[Response.With[String]] = request.parse[String]
requestContent: monix.eval.Task[io.taig.communicator.request.Response.With[String]] = BindAsync(<function3>,<function1>)

scala> // Kick off the actual request
     | import monix.execution.Scheduler.Implicits.global
import monix.execution.Scheduler.Implicits.global

scala> import scala.util.{ Failure, Success }
import scala.util.{Failure, Success}

scala> requestContent.runAsync.andThen {
     |     case Success( content ) => "Success"
     |     case Failure( exception ) => "Failure"
     | }
res5: monix.execution.CancelableFuture[io.taig.communicator.request.Response.With[String]] = monix.execution.CancelableFuture$Implementation@16a5d1fd
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
docker pull taig/communicator
docker build -t taig/communicator .
docker --entrypoint="./test.sh" taig/communicator
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
