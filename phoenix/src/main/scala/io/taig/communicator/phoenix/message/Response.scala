package io.taig.communicator.phoenix.message

import io.circe.{ Decoder, Json }
import io.circe.generic.semiauto._
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

case class Response(
    topic:   Topic,
    event:   Event,
    payload: Option[Response.Payload],
    ref:     Ref
)

object Response {
    case class Payload( status: Status, response: Json )

    object Payload {
        implicit val decoderPayload: Decoder[Option[Payload]] = {
            Decoder.instance { cursor ⇒
                val patched = cursor.withFocus {
                    case json if json.asObject.exists( _.size == 0 ) ⇒
                        Json.Null
                    case json ⇒ json
                }

                Decoder.decodeOption( deriveDecoder[Payload] )
                    .decodeJson( patched.focus )
            }
        }
    }

    sealed case class Status( value: String )

    object Status {
        object Ok extends Status( "ok" )
        object Error extends Status( "error" )

        implicit val decoderStatus: Decoder[Status] = {
            Decoder[String].map {
                case "ok"  ⇒ Ok
                case value ⇒ Status( value )
            }
        }
    }
}