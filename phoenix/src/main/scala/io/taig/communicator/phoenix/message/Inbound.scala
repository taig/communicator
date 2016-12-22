package io.taig.communicator.phoenix.message

import cats.syntax.either._
import io.circe.generic.JsonCodec
import io.circe.{ Decoder, DecodingFailure, Json }
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

sealed trait Inbound extends Product with Serializable {
    def topic: Topic
}

object Inbound {
    implicit val decoder: Decoder[Inbound] = Decoder.instance { cursor ⇒
        ( for {
            event ← cursor.get[Event]( "event" )
            topic ← cursor.get[Topic]( "topic" )
            payload = cursor.downField( "payload" )
            status ← payload.get[Option[String]]( "status" )
            ref ← cursor.get[Option[Ref]]( "ref" )
        } yield ( event, status, topic, payload, ref ) ).flatMap {
            case ( Event.Reply, Some( "ok" ), topic, payload, Some( ref ) ) ⇒
                payload
                    .get[Json]( "response" )
                    .map( Response.Confirmation( topic, _, ref ) )
            case ( Event.Reply, Some( "error" ), topic, payload, Some( ref ) ) ⇒
                payload
                    .downField( "response" )
                    .get[String]( "reason" )
                    .map( Response.Error( topic, _, ref ) )
            case ( Event.Reply, Some( status ), _, _, _ ) ⇒
                val message = s"Invalid status: $status"
                Left( DecodingFailure( message, cursor.history ) )
            case ( event, None, topic, payload, None ) ⇒
                payload.as[Json].map( Push( topic, event, _ ) )
            case ( event, _, _, _, _ ) ⇒
                val message = s"Invalid event: $event"
                Left( DecodingFailure( message, cursor.history ) )
        }
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
}

case class Push(
    topic:   Topic,
    event:   Event,
    payload: Json
) extends Inbound