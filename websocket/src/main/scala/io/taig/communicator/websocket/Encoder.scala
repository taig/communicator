package io.taig.communicator.websocket

import okhttp3.RequestBody
import okhttp3.ws.WebSocket._

trait Encoder[T] {
    def encode( value: T ): RequestBody
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