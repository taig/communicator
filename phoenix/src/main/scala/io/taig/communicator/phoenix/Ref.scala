package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }

case class Ref( value: String ) extends AnyVal

object Ref {
    implicit val encoderRef: Encoder[Ref] = Encoder[String].contramap( _.value )

    implicit val decoderRef: Decoder[Ref] = Decoder[String].map( Ref( _ ) )
}