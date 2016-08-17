//package io.taig.communicator.phoenix
//
//import io.circe.Json
//import io.taig.communicator.phoenix.Channel.EventPayload
//import io.taig.communicator.phoenix.message.Request
//import io.taig.communicator.phoenix.message.Response.Payload
//import io.taig.communicator.websocket.WebSocketChannels
//import io.taig.communicator.websocket.WebSocketChannels.Sender
//import monix.reactive.Observable
//
//class Channel( phoenix: Phoenix, val topic: Topic )
//        extends WebSocketChannels[EventPayload, Payload] {
//    private def createRequest( value: EventPayload ) = value match {
//        case ( event, payload ) ⇒
//            Request(
//                topic,
//                Event( event ),
//                payload,
//                phoenix.ref
//            )
//    }
//
//    override val sender: Sender[EventPayload] = new Sender[EventPayload] {
//        override def send( value: EventPayload ) = {
//            phoenix.websocket.sender.send( createRequest( value ) )
//        }
//
//        override def ping( value: Option[EventPayload] ) = {
//            phoenix.websocket.sender.ping(
//                value.map( createRequest )
//            )
//        }
//
//        override def close( code: Int, reason: String ) = {
//            phoenix.websocket.sender.close( code, reason )
//        }
//    }
//
//    override val receiver: Observable[Payload] = {
//        phoenix.websocket.receiver
//            .filter( _.topic == topic )
//            .map( _.payload )
//    }
//
//    def leave() = {
//        logger.info( s"Leaving channel $topic" )
//
//        val request = Request(
//            topic,
//            Event.Leave,
//            Json.Null,
//            phoenix.ref
//        )
//
//        phoenix.websocket.sender.send( request )
//    }
//
//    override def close() = leave()
//}
//
//object Channel {
//    type EventPayload = ( String, Json )
//}