# Communicator

[![Circle CI](https://circleci.com/gh/Taig/Communicator.svg?style=shield)](https://circleci.com/gh/Taig/Communicator)
[![codecov](https://codecov.io/gh/Taig/Communicator/branch/develop-0.3.0/graph/badge.svg)](https://codecov.io/gh/Taig/Communicator)

> A [`monix.Task`][3] wrapper for [OkHttp][1]

Communicator provides a simple way to construct OkHttp requests as `monix.Task`s which is equipped with a beautiful functional interface and comes with first class support for cancellation.

## Index

1. [Installation](#installation)
2. [Quickstart](#quickstart)
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

`libraryDependencies += "io.taig" %% "communicator" % "3.0.0-SNAPSHOT"`

## Quickstart

**Prerequisites**
```tut
import io.taig.communicator.Request
import monix.eval.Task
import okhttp3.OkHttpClient
import okhttp3.Request.Builder

// To build request tasks, an implicit OkHttpClient should be in scope
implicit val client = new OkHttpClient()

// Simple OkHttp request builder
val builder = new Builder().url( "http://taig.io/" )
```

## Communicator 2.x

Loren Ipsum

## Communicator 1.x

The Java predecessor of this library has been deprecated. You still can [access][2] the source and documentation, though.

## License

The MIT License (MIT)  
Copyright (c) 2016 Niklas Klein <mail@taig.io>

[1]: http://square.github.io/okhttp/
[2]: https://github.com/Taig/Communicator/tree/f820d08b1cc4d77083e384568ce89223e53ab693
[3]: https://monix.io/