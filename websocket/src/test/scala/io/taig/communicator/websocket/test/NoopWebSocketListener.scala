package io.taig.communicator.websocket.test

import java.io.IOException

import io.taig.communicator.websocket._

class NoopWebSocketListener[T]( socket: OkHttpWebSocket )
        extends WebSocketListener[T]( socket ) {
    override def onMessage( message: T ) = {}

    override def onPong( payload: Option[T] ) = {}

    override def onClose( code: Int, reason: Option[String] ) = {}

    override def onFailure( exception: IOException, response: Option[T] ) = {}
}

object NoopWebSocketListener {
    def apply[T]: OkHttpWebSocket ⇒ NoopWebSocketListener[T] = {
        new NoopWebSocketListener[T]( _ )
    }
}