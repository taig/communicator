package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest

import scala.language.implicitConversions

trait Suite extends io.taig.communicator.request.Suite {
    val request = new OkHttpRequest.Builder()
        .url( "wss://echo.websocket.org" )
        .build()
}