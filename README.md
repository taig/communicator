# Communicator

[![CircleCI](https://circleci.com/gh/Taig/communicator/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/communicator/tree/master)
[![codecov](https://codecov.io/github/Taig/Communicator/coverage.svg?branch=master)](https://codecov.io/github/Taig/communicator?branch=master)
[![Maven](https://img.shields.io/maven-central/v/io.taig/communicator_2.11.svg)](http://search.maven.org/#artifactdetails%7Cio.taig%7Ccommunicator_2.11%7C3.0.0%7Cjar)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/Taig/Communicator/master/LICENSE)

> A [monix][1] wrapper for [OkHttp][2]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Installation

```scala
libraryDependencies ++=
    "io.taig" %% "communicator-common" % "3.0.0-RC12" ::
    "io.taig" %% "communicator-request" % "3.0.0-RC12" ::
    "io.taig" %% "communicator-phoenix" % "3.0.0-RC12" ::
    Nil
```

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.0.0-RC12"
```

## Quickstart

```scala
import monix._; import eval.Task; import execution.Scheduler.Implicits.global
import io.taig.communicator._; import request._
import okhttp3.OkHttpClient
import scala._; import util._; import concurrent._; import duration._
import language.postfixOps

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = new OkHttpClient()

// Simple OkHttp request builder
val builder = new OkHttpRequest.Builder().url( "http://taig.io/" )

// Construct a Task[Response] and parse it to a String
val request = Request( builder.build() ).parse[String]
```

```scala
// Kick off the actual request
val response = request.runAsync
// response: monix.execution.CancelableFuture[io.taig.communicator.request.Response.With[String]] = monix.execution.CancelableFuture$Implementation@3400550b

Await.result( response, 3 seconds )
// res7: io.taig.communicator.request.Response.With[String] =
// >>> http://taig.io/
// [No headers]
// <<< 200 OK
// Server: GitHub.com
// Content-Type: text/html; charset=utf-8
// Last-Modified: Tue, 24 Feb 2015 15:20:41 GMT
// Access-Control-Allow-Origin: *
// Expires: Mon, 26 Dec 2016 09:42:36 GMT
// Cache-Control: max-age=600
// X-GitHub-Request-Id: B91F1118:2D53:8B3185F:5860E3B4
// Accept-Ranges: bytes
// Date: Mon, 26 Dec 2016 09:41:21 GMT
// Via: 1.1 varnish
// Age: 474
// Connection: keep-alive
// X-Served-By: cache-fra1245-FRA
// X-Cache: HIT
// X-Cache-Hits: 2
// X-Timer: S1482745281.902766,VS0,VE0
// Vary: Accept-Encoding
// X-Fastly-Request-ID: 1b951d37871fe961a5319be29e0b4f9f5f4d25d2
```

## Usage

Lorem Ipsum

### Building Requests

Lorem Ipsum

### Parsing Content

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
