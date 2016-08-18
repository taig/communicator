package io.taig.communicator.phoenix.message

import io.circe.{ Decoder, Json }
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

case class Response(
    topic:   Topic,
    event:   Event,
    payload: Response.Payload,
    ref:     Ref
)

object Response {
    case class Payload( status: Status, response: Json )

    sealed case class Status( value: String )

    object Status {
        object Ok extends Status( "ok" )

        implicit val decoderStatus: Decoder[Status] = {
            Decoder[String].map {
                case "ok"  ⇒ Ok
                case value ⇒ Status( value )
            }
        }
    }
}