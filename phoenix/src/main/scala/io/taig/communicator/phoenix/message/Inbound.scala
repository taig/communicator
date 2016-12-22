package io.taig.communicator.phoenix.message

import cats.syntax.either._
import io.circe.generic.JsonCodec
import io.circe.{ Decoder, DecodingFailure, Json }
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

sealed trait Inbound extends Product with Serializable {
    def topic: Topic
}

object Inbound {
    def unapply( inbound: Inbound ): Option[Topic] = {
        Some( inbound.topic )
    }
}

sealed trait Response extends Inbound {
    def ref: Ref
}

object Response {
    case class Confirmation(
        topic:   Topic,
        payload: Json,
        ref:     Ref
    ) extends Response

    case class Error(
        topic:   Topic,
        message: String,
        ref:     Ref
    ) extends Response

    implicit val decoder: Decoder[Response] = Decoder.instance { cursor ⇒
        ( for {
            event ← cursor.downField( "event" ).get[Event]( "event" )
            status ← cursor.downField( "payload" ).get[String]( "status" )
            topic ← cursor.get[Topic]( "topic" )
            response = cursor.downField( "payload" ).downField( "response" )
            ref ← cursor.get[Ref]( "ref" )
        } yield ( event, status, topic, response, ref ) ).flatMap {
            case ( Event.Reply, "ok", topic, response, ref ) ⇒
                response.as[Json].map( Confirmation( topic, _, ref ) )
            case ( Event.Reply, "error", topic, response, ref ) ⇒
                response.get[String]( "reason" ).map( Error( topic, _, ref ) )
            case ( Event.Reply, status, _, _, _ ) ⇒
                val message = s"Invalid status: $status"
                Left( DecodingFailure( message, cursor.history ) )
            case ( event, _, _, _, _ ) ⇒
                val message = s"Invalid event: $event"
                Left( DecodingFailure( message, cursor.history ) )
        }
    }
}

@JsonCodec
case class Push(
    topic:   Topic,
    event:   Event,
    payload: Json
) extends Inbound