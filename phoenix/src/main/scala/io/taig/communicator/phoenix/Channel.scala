package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.phoenix.Channel.EventPayload
import io.taig.communicator.phoenix.message.Request
import io.taig.communicator.phoenix.message.Response.Payload
import io.taig.communicator.websocket.WebSocketChannels
import monix.reactive.Observable
import io.circe.syntax._

class Channel( phoenix: Phoenix, val topic: Topic ) {
    private def createRequest( value: EventPayload ) = value match {
        case ( event, payload ) ⇒
            Request(
                topic,
                Event( event ),
                payload,
                phoenix.ref
            )
    }

    private[phoenix] def send( request: Request ): Unit = {
        //        phoenix.channels.writer.send( request.asJson )
    }

    private[phoenix] def send( event: Event, payload: Json ): Unit = {
        send( Request( topic, event, payload, phoenix.ref ) )
    }

    def send( event: String, payload: Json ): Unit = {
        send( Event( event ), payload )
    }

    val reader: Observable[Payload] = {
        ???
    }

    //    override val receiver: Observable[Payload] = {
    //        phoenix.websocket.receiver
    //            .filter( _.topic == topic )
    //            .map( _.payload )
    //    }

    def leave(): Unit = {
        logger.info( s"Leaving channel $topic" )
        send( Event.Leave, Json.Null )
    }
}

object Channel {
    type EventPayload = ( String, Json )
}