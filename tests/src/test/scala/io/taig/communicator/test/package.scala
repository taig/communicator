package io.taig.communicator

import io.taig.communicator.websocket._

import scala.util.Try

package object test {
    implicit val encoderInt: Encoder[Int] = Encoder.instance(
        value ⇒ Encoder[String].encode( value.toString )
    )

    implicit val decoderInt: Decoder[Int] = Decoder.instance(
        data ⇒ Try( new String( data ).toInt )
    )
}