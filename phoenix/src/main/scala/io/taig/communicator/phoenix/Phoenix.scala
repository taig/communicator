package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator._
import io.taig.communicator.phoenix.message.Response.Payload
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.websocket.WebSocketChannels
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.OverflowStrategy

import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix(
        channels:  WebSocketChannels[Response, Request],
        heartbeat: Option[Duration]
)(
        implicit
        scheduler: Scheduler
) {
    private val iterator: Iterator[Ref] = {
        Stream.iterate( 0L )( _ + 1 ).map( Ref( _ ) ).iterator
    }

    private[phoenix] def ref = synchronized( iterator.next() )

    private[phoenix] def withRef[T]( f: Ref ⇒ T ): T = f( ref )

    private[phoenix] val reader = {
        channels.reader.collect {
            case websocket.Event.Message( response ) ⇒ response
        }.publish
    }

    private[phoenix] val writer = channels.writer

    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel] = withRef { ref ⇒
        val send = Task {
            logger.info( s"Requesting to join channel $topic" )
            val request = Request( topic, Event.Join, payload, ref )
            writer.send( request )
            reader.connect()
        }

        val receive = reader.collect {
            case Response( `topic`, _, Payload( "ok", _ ), `ref` ) ⇒
                logger.info( s"Successfully joined channel $topic" )
                new Channel( this, topic )
        }.firstL

        for {
            _ ← send
            receive ← receive
        } yield receive
    }

    def close(): Unit = {
        logger.debug( "Closing" )
        channels.close()
    }
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[websocket.Event[Response]],
        heartbeat: Option[Duration]                                        = Some( 7 seconds )
    )(
        implicit
        client:    Client,
        scheduler: Scheduler
    ): Phoenix = {
        val channels = WebSocketChannels[Response, Request]( request, strategy )
        new Phoenix( channels, heartbeat )
    }
}