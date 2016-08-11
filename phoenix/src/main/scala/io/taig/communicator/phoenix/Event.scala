package io.taig.communicator.phoenix

import cats.data.Xor
import io.circe.{ Decoder, Encoder }

sealed case class Event( name: String )

object Event {
    object Close extends Event( "phx_close" )
    object Error extends Event( "phx_error" )
    object Join extends Event( "phx_join" )
    object Reply extends Event( "phx_reply" )
    object Leave extends Event( "phx_leave" )

    val all = Close :: Error :: Join :: Reply :: Leave :: Nil

    implicit val encoderEvent: Encoder[Event] = Encoder[String].contramap( _.name )

    implicit val decoderEvent: Decoder[Event] = {
        Decoder[String].emap { name ⇒
            Xor.fromOption(
                all.find( _.name == name ),
                s"Event $name does not exist"
            )
        }
    }
}