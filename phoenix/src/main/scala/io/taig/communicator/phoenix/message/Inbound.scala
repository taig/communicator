package io.taig.communicator.phoenix.message

import io.circe.{ Decoder, Json }
import io.circe.generic.semiauto._
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

sealed trait Inbound extends Product with Serializable {
    def topic: Topic

    def event: Event
}

object Inbound {
    def unapply( inbound: Inbound ): Option[( Topic, Event )] = {
        Some( inbound.topic, inbound.event )
    }
}

case class Response(
    topic:   Topic,
    event:   Event,
    payload: Option[Response.Payload],
    ref:     Ref
) extends Inbound

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
        object Error extends Status( "error" )
        object Ok extends Status( "ok" )

        implicit val decoderStatus: Decoder[Status] = {
            Decoder[String].map {
                case "error" ⇒ Error
                case "ok"    ⇒ Ok
                case value   ⇒ Status( value )
            }
        }
    }
}

case class Push(
    topic:   Topic,
    event:   Event,
    payload: Json
) extends Inbound