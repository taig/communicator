package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }

case class Ref( value: Long ) extends AnyVal

object Ref {
    implicit val encoderRef: Encoder[Ref] = Encoder[Long].contramap( _.value )

    implicit val decoderRef: Decoder[Ref] = Decoder[Long].map( Ref( _ ) )
}