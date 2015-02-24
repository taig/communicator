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
 1. [Basics](#basics)
 2. [Requests and Responses](#requests-and-responses)
 3. [Parser](#parser)
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

Request( "http://www.scala-lang.org/" )    // Prepare meta data (okhttp.Request.Builder)
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

### Basics

TODO

### Requests and Responses

TODO

### Parser

TODO

## Android

TODO

## License

The MIT License (MIT)  
Copyright (c) 2015 Niklas Klein <my.taig@gmail.com>

[1]: http://square.github.io/okhttp/
