package io.taig.communicator.websocket

import okhttp3.RequestBody
import okhttp3.ws.WebSocket._
import okio.Buffer

trait Encoder[T] {
    def encode( value: T ): RequestBody

    final def buffer( value: T ): Buffer = {
        val sink = new Buffer
        val request = encode( value )

        try {
            request.writeTo( sink )
            sink
        } finally {
            sink.close()
        }
    }
}

object Encoder {
    @inline
    def apply[T]( implicit e: Encoder[T] ): Encoder[T] = e

    def instance[T]( e: T ⇒ RequestBody ): Encoder[T] = {
        new Encoder[T] {
            override def encode( value: T ) = e( value )
        }
    }

    implicit val encoderString: Encoder[String] = {
        instance( RequestBody.create( TEXT, _ ) )
    }
}