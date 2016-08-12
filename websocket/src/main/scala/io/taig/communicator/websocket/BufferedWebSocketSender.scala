package io.taig.communicator.websocket

import okio.Buffer

private class BufferedWebSocketSender[T: Codec]( buffer: BufferedOkHttpWebSocket )
        extends WebSocket.Sender[T] {
    override def send( value: T ) = {
        buffer.sendMessage( Codec[T].encode( value ) )
    }

    override def ping( value: Option[T] ) = {
        val sink = value.map { value ⇒
            val sink = new Buffer
            val request = Codec[T].encode( value )

            try {
                request.writeTo( sink )
                sink
            } finally {
                sink.close()
            }
        }

        buffer.sendPing( sink.orNull )
    }

    override def close( code: Int, reason: String ) = {
        buffer.close( code, reason )
    }
}