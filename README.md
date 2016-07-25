# Communicator

[![CircleCI](https://circleci.com/gh/Taig/communicator/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/communicator/tree/master)
[![codecov](https://codecov.io/github/Taig/Communicator/coverage.svg?branch=master)](https://codecov.io/github/Taig/Communicator?branch=master)
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
libraryDependencies ++=
    "io.taig" %% "communicator-request" % "3.0.0-SNAPSHOT" ::
    "io.taig" %% "communicator-websocket" % "3.0.0-SNAPSHOT" ::
    Nil
```

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.0.0-SNAPSHOT"
```

## Quickstart

```scala
scala> import io.taig.communicator._; import monix.eval.Task
import io.taig.communicator._
import monix.eval.Task

scala> // To build request tasks, an implicit OkHttpClient should be in scope
     | implicit val client = Client()
client: io.taig.communicator.Client = okhttp3.OkHttpClient@5ff72814

scala> // Simple OkHttp request builder
     | val builder = Request.Builder().url( "http://taig.io/" )
builder: okhttp3.Request.Builder = okhttp3.Request$Builder@6fc82ed4

scala> // Construct a Task[Response]
     | val request: Request = Request( builder.build() )
request: io.taig.communicator.Request = io.taig.communicator.Request@2dae3ab7

scala> // Parse the response to a String
     | val requestContent: Task[Response.With[String]] = request.parse[String]
requestContent: monix.eval.Task[io.taig.communicator.Response.With[String]] = BindAsync(<function3>,<function1>)

scala> // Kick off the actual request
     | import monix.execution.Scheduler.Implicits.global
import monix.execution.Scheduler.Implicits.global

scala> import scala.util.{ Failure, Success }
import scala.util.{Failure, Success}

scala> requestContent.runAsync.andThen {
     |     case Success( content ) => "Success"
     |     case Failure( exception ) => "Failure"
     | }
res5: monix.execution.CancelableFuture[io.taig.communicator.Response.With[String]] = monix.execution.CancelableFuture$Implementation@74484cf
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
