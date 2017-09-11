# Communicator

[![CircleCI](https://circleci.com/gh/Taig/communicator/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/communicator/tree/master)
[![codecov](https://codecov.io/gh/Taig/communicator/branch/master/graph/badge.svg)](https://codecov.io/gh/Taig/communicator)
[![Maven](https://img.shields.io/maven-central/v/io.taig/communicator_2.12.svg)](http://search.maven.org/#artifactdetails%7Cio.taig%7Ccommunicator_2.12%7C3.3.0-RC2%7Cjar)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/Taig/Communicator/master/LICENSE)

> A [monix][1] wrapper for [OkHttp][2]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Installation

```scala
libraryDependencies ++=
    "io.taig" %% "communicator-request" % "3.3.0-RC2" ::
    "io.taig" %% "communicator-builder" % "3.3.0-RC2" ::
    Nil
```

## Quickstart

```tut:silent
import monix._; import eval.Task; import execution.Scheduler.Implicits.global
import io.taig.communicator._; import request._
import okhttp3.OkHttpClient
import scala._; import util._; import concurrent._; import duration._

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = new OkHttpClient()

// Simple OkHttp request builder
val builder = new OkHttpRequest.Builder().url( "https://github.com/" )

// Construct a Task[Response] and parse it to a String
val request = Request( builder.build() ).parse[String]

// Kick off the actual request
val response = request.runAsync
```

```tut:book
Await.result( response, 30.seconds )
```

## Usage

Communicator provides a thin layer around OkHttp using `monix.Task` to execute HTTP requests and `monix.Observable` for Phoenix Channels. To construct requests, the OkHttp builder API is used.

### Building Requests

Use the [OkHttp builder API][2] to construct requests which are then lifted into `io.taig.communicator.request.Request`.

```tut:silent
val headers = new OkHttpRequest.Builder().
    url( "https://github.com/" ).
    header( "X-API-Key", "foobar" ).
    build()

val request: Request = Request( headers )
```

### Handling Responses

There are several ways to transform a `Request` to an executable `Task[Response]`.

```tut:silent
// Ignores response body
val ignoreBody: Task[Response[Unit]] = request.ignoreBody

// Parses response body to a String
val parse: Task[Response[String]] = request.parse[String]
```

## Android

To use Communicator on the Android platform please extend your ProGuard rules by the following instructions:

```scala
proguardOptions ++=
    "-dontnote okhttp3.internal.**" ::
    "-dontnote monix.execution.internals.**" ::
    "-dontwarn io.circe.generic.util.macros.**" ::
    "-dontwarn monix.execution.internals.**" ::
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

## Communicator 2.x

The `scala.concurrent.Future` predecessor of this library has been deprecated. You can still [access][3] the source and documentation.

## Communicator 1.x

The Java predecessor of this library has been deprecated. You can still [access][4] the source and documentation.

[1]: https://monix.io/
[2]: http://square.github.io/okhttp/
[3]: https://github.com/Taig/Communicator/tree/2.3.2
[4]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693