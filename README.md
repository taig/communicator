# Communicator

> An [OkHttp][1] wrapper for Scala built with Android in mind

Communicator provides a simple `scala.concurrent.Future` implementation that handles your requests based on plain OkHttp request and client objects. Additional callbacks (e.g. to track upload and dowload progress) simplify your codebase tremendously.

Communicator was built for Android, but has no dependencies to the framework and works fine with any Scala project.

**Highlights**

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
 3. [Request: Plain, Parse and Handle](#parser)
 4. [Event Callbacks](#event-callbacks)
 5. [Response](#response)
4. [Android](#android)
5. [License](#license)

## Installation

*Communicator* is available via Maven Central

`libraryDependencies += "io.taig" %% "communicator" % "1.0.0"`

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

Request( "http://www.scala-lang.org/" )    // Prepare metadata (okhttp.Request.Builder)
	.parse[String]()                       // Start request in parse mode
	.onReceive( println )( single )        // Execute callback on single ExecutionContext
	.onSuccess( response =>
	{
		import response._
		println( "######################" )
		println( s"$code $message" )
		println( s"${payload.take( 30 )}...${payload.takeRight( 30 )}" )
	} )
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
######################
200 OK
<!DOCTYPE html><html>  <head> ...ipt"></script>  </body></html>
````

## Usage

### Prerequisites

As already shown in the [Getting Started][#getting-started] section, you'll need to import the *Communicator* API via:

````scala
import io.taig.communicator._
````

Furthermore Scala Futures require an implicit `ExecutionContext` in scope, the suggested default is:

````scala
import scala.concurrent.ExecutionContext.Implicits.global
````

Each request requires an `OkHttpClient` instance to be passed along. The recommended way is to provide it as an implicit, but there are also factory methods available that allow to specifiy the client explicitly.

> **Please Note**  
This section is very important for Android useres. To learn how to work with the default Android thread pool and the UI thread, please read the [Android](#android) section.

### Metadata

Each *Communicator* request is based on an `okhttp.Request` that you need to prepare in advance. This library provides helpers to simplify the construction. All of the below yield the same result and should give you a feeling of the available implicit conversions!

````scala
import com.squareup.okhttp

new okhttp.Request.Builder()
  .url( "http://www.scala-lang.org/" )
  .build()
````

````scala
Request()
  .url( "http://www.scala-lang.org/" )
  .build()
````

````scala
Request( "http://www.scala-lang.org/" ).build()
````

### Request: Plain, Parse and Handle

There are three different request types, depending on your use case.

#### `Request.Plain`

This is the simplest type. A plain request does not gather any information about the response body. This may be useful for API write requests where you only care about the response code or an HTTP Head request.

````scala
Request.plain( request )
````

#### `Request.Parser[T]`

Processes the response body to yield an object of type `T`. You need to bring an implicit parser in scope. By default *Communicator* is equipped with a `String` parser.

To create a custom parser you have to implement `io.taig.communicator.Parser`:

````scala
trait Parser[T]
{
  def parse( response: Response, stream: InputStream ): T
}
````

A sample implementation that parses the response body to JSON:

````scala
import play.api.libs.json.Json

object Json extends Parser[JsValue]
{
  def parse( response: Response, stream: InputStream ): JsValue =
  {
    Json.parse( stream )
  }
}
````

````scala
implicit def parser = Json

Request.parse[Json]( request )  // Implicit parser
Request.parse( request, Json )  // Explicit parser
````

#### `Request.Handler`

Is very similar to `Request.Parser[T]`, in fact you can think of it as a special case for `Request.Parser[Unit]`. Handler is for response body processing where you don't care about the result (e.g. forward data to some log file). You need to bring an implicit handler in scope.

To create a custom handler you have to implement `io.taig.communicator.Handler`:

````scala
trait Handler
{
  def handle( response: Response, stream: InputStream ): Unit
}
````

````scala
implicit def handler = MyLogFileHandler

Request.handle( request )                    // Implicit parser
Request.handle( request, MyLogFileHandler )  // Explicit parser
````

### Event Callbacks

TODO

### Response

TODO

## Android

TODO

## License

The MIT License (MIT)  
Copyright (c) 2015 Niklas Klein <my.taig@gmail.com>

[1]: http://square.github.io/okhttp/
