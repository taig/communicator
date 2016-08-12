package io.taig.communicator

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

package object websocket {
    type OkHttpWebSocket = okhttp3.ws.WebSocket

    private[websocket] val logger = {
        Logger( LoggerFactory.getLogger( "websocket" ) )
    }
}