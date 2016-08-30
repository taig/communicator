package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.phoenix.message.{ Inbound, Request }
import monix.reactive.Observable

class Channel( phoenix: Phoenix, val reader: Observable[Inbound], val topic: Topic ) { self ⇒
    private[phoenix] def send( request: Request ): Unit = {
        phoenix.writer.send( request )
    }

    private[phoenix] def send( event: Event, payload: Json ): Unit = {
        send( Request( topic, event, payload, phoenix.ref ) )
    }

    //    val reader: Observable[Inbound] = {
    //        phoenix.reader.filter { inbound ⇒
    //            topic isSubscribedTo inbound.topic
    //        }
    //    }

    val writer: ChannelWriter = new ChannelWriter {
        override def send( event: String, payload: Json ) = {
            self.send( Event( event ), payload )
        }
    }

    def leave(): Unit = {
        logger.info( s"Leaving channel $topic" )
        send( Event.Leave, Json.Null )
    }

    def close(): Unit = {
        leave()
        phoenix.close()
    }
}