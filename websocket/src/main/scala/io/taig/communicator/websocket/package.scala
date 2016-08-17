package io.taig.communicator

import com.typesafe.scalalogging.Logger
import monix.reactive.Observable
import org.slf4j.LoggerFactory

package object websocket {
    type OkHttpWebSocket = okhttp3.ws.WebSocket

    type OkHttpWebSocketListener = okhttp3.ws.WebSocketListener

    //    type WebSocketReader[T] = Observable[Event[T]]

    private[websocket] val logger = {
        Logger( LoggerFactory.getLogger( "websocket" ) )
    }
}