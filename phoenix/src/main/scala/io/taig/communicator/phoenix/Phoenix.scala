package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator._
import io.taig.communicator.phoenix.message.Response.{ Payload, Status }
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.websocket.{ WebSocketChannels, WebSocketWriter }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.OverflowStrategy

import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix(
        channels:  WebSocketChannels[Response, Request],
        heartbeat: Option[FiniteDuration]
)(
        implicit
        scheduler: Scheduler
) {
    private val iterator: Iterator[Ref] = {
        Stream.iterate( 0L )( _ + 1 ).map( Ref( _ ) ).iterator
    }

    private var periodicHeartbeat: Option[Cancelable] = None

    private[phoenix] def ref = synchronized( iterator.next() )

    private[phoenix] def withRef[T]( f: Ref ⇒ T ): T = f( ref )

    /**
     * A Reader that only cares about message events
     *
     * Also the heartbeat is started with the first subscription.
     */
    private[phoenix] val reader = {
        channels.reader.collect {
            case websocket.Event.Message( response ) ⇒ response
        }.doOnStart { _ ⇒
            heartbeat.foreach( startHeartbeat )
        }.publish
    }

    /**
     * A Writer that proxies the Channel Writer in order the reschedule
     * the heartbeat
     */
    private[phoenix] val writer = {
        heartbeat.fold[WebSocketWriter[Request]]( channels.writer ) { heartbeat ⇒
            new HeartbeatWebSocketWriterProxy(
                channels.writer,
                () ⇒ startHeartbeat( heartbeat ),
                stopHeartbeat
            )
        }
    }

    private def startHeartbeat( heartbeat: FiniteDuration ): Unit = synchronized {
        logger.debug( "Starting heartbeat" )

        periodicHeartbeat.foreach { heartbeat ⇒
            logger.warn( "Overriding existing heartbeat" )
            heartbeat.cancel()
        }

        val scheduler = Scheduler.singleThread( "heartbeat" )

        val cancelable = scheduler.scheduleWithFixedDelay( heartbeat, heartbeat ) {
            logger.debug( "Sending heartbeat" )

            channels.writer.sendNow {
                Request(
                    Topic( "phoenix" ),
                    Event( "heartbeat" ),
                    Json.Null,
                    ref
                )
            }
        }

        periodicHeartbeat = Some( cancelable )
    }

    private def stopHeartbeat(): Unit = synchronized {
        logger.debug( "Stopping heartbeat" )

        periodicHeartbeat.foreach( _.cancel() )
        periodicHeartbeat = None
    }

    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel] = withRef { ref ⇒
        val send = Task {
            logger.info( s"Requesting to join channel $topic" )
            val request = Request( topic, Event.Join, payload, ref )
            writer.send( request )
            reader.connect()
        }

        val receive = reader.collect {
            case Response( `topic`, _, Payload( Status.Ok, _ ), `ref` ) ⇒
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
        stopHeartbeat()
        channels.close()
    }
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[websocket.Event[Response]] = websocket.Default.strategy,
        heartbeat: Option[FiniteDuration]                                  = Default.heartbeat
    )(
        implicit
        client:    Client,
        scheduler: Scheduler
    ): Phoenix = {
        val channels = WebSocketChannels[Response, Request]( request, strategy )
        new Phoenix( channels, heartbeat )
    }
}

private class HeartbeatWebSocketWriterProxy(
        writer: WebSocketWriter[Request],
        start:  () ⇒ Unit,
        stop:   () ⇒ Unit
) extends WebSocketWriter[Request] {
    override def send( value: Request ) = {
        stop()
        writer.send( value )
        start()
    }

    override def ping( value: Option[Request] ) = {
        stop()
        writer.ping( value )
        start()
    }

    override def close( code: Int, reason: Option[String] ) = {
        stop()
        writer.close( code, reason )
    }
}