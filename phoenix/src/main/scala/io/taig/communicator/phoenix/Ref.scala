package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }

case class Ref( value: String ) extends AnyVal

object Ref {
    implicit val encoder: Encoder[Ref] = Encoder[String].contramap( _.value )

    implicit val decoder: Decoder[Ref] = Decoder[String].map( Ref( _ ) )

    /**
     * Endless stream of incrementing Refs (starting at 0)
     */
    private val iterator: Iterator[Ref] = {
        Stream
            .iterate( 0L )( _ + 1 )
            .map( n ⇒ Ref( n.toString ) )
            .iterator
    }

    /**
     * Get a fresh, unique Ref
     */
    def unique(): Ref = synchronized( iterator.next() )
}