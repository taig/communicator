package io.taig.communicator

import io.taig.communicator.websocket.Codec

import scala.util.Try

package object test {
    implicit val codecInt: Codec[Int] = Codec.instance(
        value ⇒ Codec[String].encode( value.toString ),
        data ⇒ Try( new String( data ).toInt )
    )
}