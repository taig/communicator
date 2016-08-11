package io.taig.communicator.websocket

import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT

import scala.util.Try

trait Codec[T] {
    def encode( value: T ): RequestBody

    def decode( value: Array[Byte] ): Try[T]
}

object Codec {
    @inline
    def apply[T]( implicit c: Codec[T] ): Codec[T] = c

    def instance[T]( e: T ⇒ RequestBody, d: Array[Byte] ⇒ Try[T] ): Codec[T] = {
        new Codec[T] {
            override def encode( value: T ) = e( value )

            override def decode( value: Array[Byte] ) = d( value )
        }
    }

    implicit val codecString: Codec[String] = instance(
        RequestBody.create( TEXT, _ ),
        data ⇒ Try( new String( data ) )
    )
}