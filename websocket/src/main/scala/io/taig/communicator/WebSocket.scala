package io.taig.communicator

import okhttp3.{ OkHttpClient, Request }
import okhttp3.ws.WebSocketCall

object WebSocket {
    def apply( request: Request )( implicit c: OkHttpClient ) = {
        WebSocketCall.create( c, request )
    }
}