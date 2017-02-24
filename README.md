# Communicator

[![CircleCI](https://circleci.com/gh/Taig/communicator/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/communicator/tree/master)
[![codecov](https://codecov.io/gh/Taig/communicator/branch/master/graph/badge.svg)](https://codecov.io/gh/Taig/communicator)
[![Maven](https://img.shields.io/maven-central/v/io.taig/communicator_2.12.svg)](http://search.maven.org/#artifactdetails%7Cio.taig%7Ccommunicator_2.12%7C3.2.0%7Cjar)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/Taig/Communicator/master/LICENSE)

> A [monix][1] wrapper for [OkHttp][2]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Installation

```scala
libraryDependencies ++=
    "io.taig" %% "communicator-common" % "3.2.0" ::
    "io.taig" %% "communicator-request" % "3.2.0" ::
    "io.taig" %% "communicator-phoenix" % "3.2.0" ::
    Nil
```

```scala
libraryDependencies += "io.taig" %% "communicator" % "3.2.0"
```

## Quickstart

```scala
import monix._; import eval.Task; import execution.Scheduler.Implicits.global
import io.taig.phoenix.models._
import io.taig.communicator._; import request._
import okhttp3.OkHttpClient
import scala._; import util._; import concurrent._; import duration._

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = new OkHttpClient()

// Simple OkHttp request builder
val builder = new OkHttpRequest.Builder().url( "http://taig.io/" )

// Construct a Task[Response] and parse it to a String
val request = Request( builder.build() ).parse[String]

// Kick off the actual request
val response = request.runAsync
```

```scala
Await.result( response, 30.seconds )
// res8: io.taig.communicator.request.Response.With[String] =
// >>> http://taig.io/
// [No headers]
// <<< 200 OK
// Server: GitHub.com
// Content-Type: text/html; charset=utf-8
// Last-Modified: Tue, 24 Feb 2015 15:20:41 GMT
// Access-Control-Allow-Origin: *
// Expires: Fri, 24 Feb 2017 07:06:13 GMT
// Cache-Control: max-age=600
// X-GitHub-Request-Id: B8A8:F8D5:4E2309:64F386:58AFD90C
// Accept-Ranges: bytes
// Date: Fri, 24 Feb 2017 12:45:51 GMT
// Via: 1.1 varnish
// Age: 0
// Connection: keep-alive
// X-Served-By: cache-fra1225-FRA
// X-Cache: HIT
// X-Cache-Hits: 1
// X-Timer: S1487940351.523424,VS0,VE92
// Vary: Accept-Encoding
// X-Fastly-Request-ID: 893f042bbff10d497bc215285c5a46d9465b7ffb
```

## Usage

Communicator provides a thin layer around OkHttp using `monix.Task` to execute HTTP requests and `monix.Observable` for Phoenix Channels. To construct requests, the OkHttp builder API is used.

### Building Requests

Use the [OkHttp builder API][2] to construct requests which are then lifted into `io.taig.communicator.request.Request`.

```scala
val headers = new OkHttpRequest.Builder().
    url( "http://taig.io/" ).
    header( "X-API-Key", "foobar" ).
    build()

val request: Request = Request( headers )
```

### Handling Responses

There are several ways to transform a `Request` to an executable `Task[Response]`.

```scala
// Ignores response body
val ignoreBody: Task[Response] = request.ignoreBody

// Parses response body to a String
val parse: Task[Response.With[String]] = request.parse[String]
```

### Phoenix Channels

```scala
import monix.execution.Scheduler.Implicits.global
import io.taig.communicator._; import websocket._; import phoenix._
import io.taig.phoenix.models._
import okhttp3.{ConnectionPool, OkHttpClient}
import scala._; import util._; import concurrent._; import duration._
import java.util.concurrent.TimeUnit

implicit val client = new OkHttpClient()

val request = new OkHttpRequest.Builder().
    url( s"ws://localhost:4000/socket/websocket" ).
    build()

val topic = Topic( "echo", "foobar" )

val websocket = WebSocket( request )
val phoenix = Phoenix( websocket )
val channel = Channel.join( topic )( phoenix )

val task = channel.collect {
    case Channel.Event.Available( channel ) ⇒ channel
}.firstL.map( _.topic )
```

```scala
// Await.result( task.runAsync, 90.seconds )
```

## Android

To use Communicator on the Android platform please extend your ProGuard rules by the following instructions:

```scala
proguardOptions ++=
    "-keepattributes EnclosingMethod,InnerClasses,Signature" ::
    "-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry" ::
    "-dontwarn javax.xml.bind.DatatypeConverter" ::
    "-dontnote org.joda.time.DateTimeZone" ::
    "-dontnote scala.concurrent.stm.impl.STMImpl$" ::
    "-dontnote okhttp3.internal.**" ::
    "-dontwarn io.circe.generic.util.macros.**" ::
    "-dontwarn monix.execution.internals.**" ::
    "-dontnote monix.execution.internals.**" ::
    "-dontwarn okio.**" ::
    "-dontwarn org.jctools.**" ::
    "-dontwarn org.slf4j.**" ::
    Nil
```

You might also use existing platform `Executor`s to provide a monix `Scheduler`:

```scala
import android.os.AsyncTask
import android.util.Log
import monix.execution.Scheduler

implicit val PoolScheduler: Scheduler = Scheduler {
    ExecutionContext.fromExecutor(
        AsyncTask.THREAD_POOL_EXECUTOR,
        t ⇒ Log.e( "PoolScheduler", "Failure during asynchronous operation", t )
    )
}
```

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
