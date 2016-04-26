# Communicator

> An [OkHttp][1] wrapper for Scala built with Android in mind

[![Circle CI](https://circleci.com/gh/Taig/Communicator.svg?style=svg)](https://circleci.com/gh/Taig/Communicator)

Communicator provides a simple `scala.concurrent.Future` implementation that handles your requests based on plain OkHttp request objects. Additional callbacks (e.g. to track upload and download progress) simplify your codebase tremendously.

Communicator was originally built for Android, but has no dependencies to the framework and works fine with any Scala project.

**Feature Highlights**

- Request class implements `scala.concurrent.Future`
- Easy progress updates with `onSend()` and `onReceive()` callbacks
- Progress updates support OkHttp's "transparent GZIP"
- Lovely Android integration due to callback `ExecutionContext` parameter

## Index

1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Usage](#usage)
 1. [Prerequisites](#prerequisites)
 2. [Metadata](#metadata)
 3. [Request](#request)
 4. [Event Callbacks](#event-callbacks)
 5. [Response](#response)
4. [Android](#android)
5. [Communicator 1.x](#communicator-1x)
5. [License](#license)

## Installation

`libraryDependencies += "io.taig" %% "communicator" % "2.3.0"`

## Getting Started

**Prerequisites**
````scala
// Get all important classes and conversions in scope
import io.taig.communicator._

// Provide implicit executors, just as using scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// Make sure an OkHttpClient is in implicit scope
implicit val client = new OkHttpClient()
````

**Create and execute**
````scala
// Single thread executor to handle progress updates
val single = ExecutionContext.fromExecutor( Executors.newSingleThreadExecutor() )

Request
    // Construct an okhttp.Request.Builder
    .prepare( "http://www.scala-lang.org/" )
    // Implicitly convert a okhttp.Request.Builder or okhttp.Request to
    // a communicator.Request and kick it off, parsing the response to a String
    .start[String]()
    // Execute callback on a single ExecutionContext to guarantee a
    // proper execution order
    .onReceive( println )( single )
    .done {
        case Response( code, body ) => println( s"$code: ${body.take( 30 )}...${body.takeRight( 30 )}" )
    }
````

**Result**
````
0 B / 12,05 KiB (0,00%)
1,00 KiB / 12,05 KiB (8,31%)
3,00 KiB / 12,05 KiB (24,91%)
5,00 KiB / 12,05 KiB (41,51%)
7,00 KiB / 12,05 KiB (58,11%)
9,00 KiB / 12,05 KiB (74,70%)
11,00 KiB / 12,05 KiB (91,30%)
12,05 KiB / 12,05 KiB (100,00%)
200: <!DOCTYPE html><html>  <head> ...ipt"></script>  </body></html>
````

## Usage

### Prerequisites

As already shown in the [Getting Started](#getting-started) section, you'll need to import the *Communicator* API via:

````scala
import io.taig.communicator._
````

Furthermore Scala Futures require an implicit `ExecutionContext` in scope, the suggested default is:

````scala
import scala.concurrent.ExecutionContext.Implicits.global
````

Each request requires an implicit `OkHttpClient` instance to be passed along.

> **Please Note**  
This section is very important for Android users. To learn how to work with the default Android thread pool and the UI thread, please read the [Android](#android) section.

### Metadata

Each *Communicator* request is based on an `okhttp.Request` that you need to prepare in advance. This library provides helpers to simplify the construction. All of the below yield the same result and should give you a feeling of the available implicit conversions!

````scala
import com.squareup.okhttp

new okhttp.Request.Builder()
    .url( "http://www.scala-lang.org/" )
````

````scala
Request
    .prepare()
    .url( "http://www.scala-lang.org/" )
````

````scala
Request
    .prepare( "http://www.scala-lang.org/" )
````

### Request

A Request can be initiated from any `okhttp.Request` or `okhttp.Request.Builder` object via the implicit `start()` method. Alternatively, the `Request.apply( request: okhttp.Request )` does the job as well. The Request class extends Scalas Future and behaves in a similar way. Once a Request has been instantiated, it starts its networking immediatly.

To handle the server response, you have to convert the `Request` object to a `Request.Payload[T]` instance. This is done via the `Request.parse[T]()` method.

The parse method expects an implicit `Parser[T]` in scope to process the server response. By default, *Communicator* provides Parsers for the `String` and `Nothing` type. Implementing your own parser is easy.

````scala
import play.api.libs.json.Json

implicit object Json extends Parser[JsValue] {
    def parse( response: Response, stream: InputStream ): JsValue = Json.parse( stream )
}
````

### Event Callbacks

Since the *Communicator* `Request` inherits from `scala.concurrent.Future`, you can rely on the default callbacks like `onComplete()` or `onSuccess()`. Furthermore *Communicator* allows you to chain event callbacks to keep your code clean. But please keep in mind that this does not necessarily insure a corresponding execution order.

> The `onComplete`, `onSuccess`, and `onFailure` methods have result type `Unit`, which means invocations of these methods cannot be chained. Note that this design is intentional, to avoid suggesting that chained invocations may imply an ordering on the execution of the registered callbacks (callbacks registered on the same future are unordered).  
— http://docs.scala-lang.org/overviews/core/futures.html

The most prominent addition to the Future API are the progress tracking callbacks `onSend()` and `onReceive()` to handle upload and download progress.

> **Please Note**  
Bear in mind that the `onSend()` callback requires you to specify an explicit content length in your request body and that `onReceive()` is only aware of the total response size if the server includes this information in its response headers!

### Response

The Response basically wraps the `okhttp.Response` and provides the same information except for the body which is only available in a `Response.Payload[T]`.

## Android

Using *Communicator* on Android does not differ from the explanations in the [Usage](#usage) section. But you can make your life a lot easier with a properly defined `ExecutionContext`.

````scala
package com.example.app

import android.os.{AsyncTask, Handler, Looper}
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

package object app {
    val Executor = new {
        // ExecutionContext for asynchronous processing, relying on Android's idea of threading
        implicit lazy val Pool = ExecutionContext.fromExecutor( AsyncTask.THREAD_POOL_EXECUTOR )

        // UI thread executor
        lazy val Ui = ExecutionContext.fromExecutor( new Executor {
            private val handler = new Handler( Looper.getMainLooper )

            override def execute( command: Runnable ) = handler.post( command )
        } )
    }
}
````

With this definition around you can now go ahead, run asynchronous HTTP requests and update your UI without cluttering your code.

````scala
import io.taig.communicator._
import com.example.app.Executor._

val dialog: android.app.ProgressDialog = ???

Request
    .prepare( "http://www.scala-lang.org/" )
    .parse[String]()
    .onReceive {
        case progress @ Progress.Receive( _, Some( _ ) ) =>
            dialog.setProgress( progress.percentage.get.toInt )
        case Progress.Receive( _, None ) => dialog.setIndeterminate( true )
    }( Ui )
    .done{ case Response( _, body ) => showConfirmationDialog( body ) }( Ui )
    .fail{ case exception => showErrorDialog( exception ) }( Ui )
    .always{ case _ => dialog.dismiss() }( Ui )
````

## *Communicator* 1.x

The Java predecessor of this library has been deprecated. You still can [access][2] the source and documentation, though.

## License

The MIT License (MIT)  
Copyright (c) 2015 Niklas Klein <mail@taig.io>

[1]: http://square.github.io/okhttp/
[2]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693
