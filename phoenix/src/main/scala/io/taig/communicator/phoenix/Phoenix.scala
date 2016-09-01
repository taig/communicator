package io.taig.communicator.phoenix

import cats.data.Xor
import cats.syntax.xor._
import io.circe.Json
import io.taig.communicator._
import io.taig.communicator.phoenix.message.Response.{ Payload, Status }
import io.taig.communicator.phoenix.message._
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.OverflowStrategy
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

trait Phoenix {
    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel]

    def close(): Unit
}

//class Phoenix(
//        channels:  WebSocketChannels[Inbound, Outbound],
//        heartbeat: Option[FiniteDuration]
//)(
//        implicit
//        scheduler: Scheduler
//) { self ⇒
//    private val iterator: Iterator[Ref] = {
//        Stream.iterate( 0L )( _ + 1 ).map( n ⇒ Ref( n.toString ) ).iterator
//    }
//
//    private var periodicHeartbeat: Option[Cancelable] = None
//
//    private def ref = synchronized( iterator.next() )
//
//    private def withRef[T]( f: Ref ⇒ T ): T = f( ref )
//
//    private val reader = {
//        channels.reader.collect {
//            case websocket.Event.Message( response ) ⇒ response
//        }.publish
//    }
//
//    private[phoenix] val writer = {
//        heartbeat.fold[WebSocketWriter[Outbound]]( channels.writer ) { heartbeat ⇒
//            new HeartbeatWebSocketWriterProxy(
//                channels.writer,
//                () ⇒ startHeartbeat( heartbeat ),
//                stopHeartbeat
//            )
//        }
//    }
//
//    private def startHeartbeat( heartbeat: FiniteDuration ): Unit = synchronized {
//        logger.debug( "Starting heartbeat" )
//
//        periodicHeartbeat.foreach { heartbeat ⇒
//            logger.warn( "Overriding existing heartbeat" )
//            heartbeat.cancel()
//        }
//
//        val scheduler = Scheduler.singleThread( "heartbeat" )
//
//        val cancelable = scheduler.scheduleWithFixedDelay( heartbeat, heartbeat ) {
//            logger.debug( "Sending heartbeat" )
//
//            channels.writer.sendNow {
//                Request(
//                    Topic( "phoenix" ),
//                    Event( "heartbeat" ),
//                    Json.Null,
//                    ref
//                )
//            }
//        }
//
//        periodicHeartbeat = Some( cancelable )
//    }
//
//    private def stopHeartbeat(): Unit = synchronized {
//        periodicHeartbeat.foreach { heartbeat ⇒
//            logger.debug( "Stopping heartbeat" )
//            heartbeat.cancel()
//        }
//        periodicHeartbeat = None
//    }
//
//    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel] = withRef { ref ⇒
//        val send = Task {
//            logger.info( s"Requesting to join channel $topic" )
//            val request = Request( topic, Event.Join, payload, ref )
//            writer.send( request )
//        }
//
//        val receive = reader.collect {
//            case Response( `topic`, _, Some( Payload( Status.Ok, _ ) ), `ref` ) ⇒
//                logger.info( s"Successfully joined channel $topic" )
//                channel( topic ).right
//            case Response( `topic`, _, Some( payload @ Payload( Status.Error, _ ) ), `ref` ) ⇒
//                logger.info( s"Failed to join channel $topic" )
//                payload.left
//        }.firstL.flatMap {
//            case Xor.Right( channel ) ⇒ Task.now( channel )
//            case Xor.Left( payload ) ⇒ Task.raiseError {
//                val error = Phoenix.error( payload ).getOrElse( "" )
//                new IllegalArgumentException {
//                    s"Failed to join channel $topic: $error"
//                }
//            }
//        }.timeout( 10 seconds )
//
//        for {
//            _ ← send
//            receive ← receive
//        } yield receive
//    }
//
//    private def channel( topic: Topic ): Channel = {
//        val reader = self.reader.filter { inbound ⇒
//            topic isSubscribedTo inbound.topic
//        }
//
//        val writer = new ChannelWriter {
//            override def send( event: Event, payload: Json ) = {
//                self.writer.send( Request( topic, event, payload, ref ) )
//            }
//        }
//
//        Channel( topic, reader, writer )
//    }
//
//    def connect(): Cancelable = {
//        reader.connect()
//    }
//
//    def close(): Unit = {
//        logger.debug( "Closing" )
//        stopHeartbeat()
//        channels.close()
//    }
//}
//
//object Phoenix {
//    def apply(
//        request:   OkHttpRequest,
//        strategy:  OverflowStrategy.Synchronous[websocket.Event[Inbound]] = websocket.Default.strategy,
//        heartbeat: Option[FiniteDuration]                                 = Default.heartbeat
//    )(
//        implicit
//        client:    OkHttpClient,
//        scheduler: Scheduler
//    ): Phoenix = {
//        val channels = WebSocketChannels[Inbound, Outbound](
//            request,
//            strategy
//        )
//        new Phoenix( channels, heartbeat )
//    }
//
//    /**
//     * Extract the error reason from a server response
//     */
//    private[phoenix] def error( payload: Payload ): Option[String] = {
//        payload.status match {
//            case Status.Error ⇒
//                payload.response.asObject
//                    .flatMap( _.apply( "reason" ) )
//                    .flatMap( _.asString )
//            case _ ⇒ None
//        }
//    }
//}
//
//private class HeartbeatWebSocketWriterProxy(
//        writer: BufferedWebSocketWriter[Outbound],
//        start:  () ⇒ Unit,
//        stop:   () ⇒ Unit
//) extends WebSocketWriter[Outbound] {
//    override def send( value: Outbound ) = {
//        stop()
//
//        writer.send( value )
//
//        if ( writer.isConnected ) {
//            start()
//        }
//    }
//
//    override def ping( value: Option[Outbound] ) = {
//        stop()
//
//        writer.ping( value )
//
//        if ( writer.isConnected ) {
//            start()
//        }
//    }
//
//    override def close( code: Int, reason: Option[String] ) = {
//        stop()
//        writer.close( code, reason )
//    }
//}