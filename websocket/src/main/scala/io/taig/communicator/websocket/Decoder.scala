package io.taig.communicator.websocket

import scala.util.Try

trait Decoder[T] {
    def decode( value: Array[Byte] ): Try[T]
}

object Decoder {
    @inline
    def apply[T]( implicit c: Decoder[T] ): Decoder[T] = c

    def instance[T]( d: Array[Byte] ⇒ Try[T] ): Decoder[T] = {
        new Decoder[T] {
            override def decode( value: Array[Byte] ) = d( value )
        }
    }

    implicit val decoderString: Decoder[String] = {
        instance( data ⇒ Try( new String( data ) ) )
    }
}