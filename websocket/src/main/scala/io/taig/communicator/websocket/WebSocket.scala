package io.taig.communicator.websocket

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.execution.cancelables.MultiAssignmentCancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okio.ByteString

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object WebSocket {
    sealed trait Event

    object Event {
        case object Connecting extends Event

        case object Reconnecting extends Event

        case class Open( socket: OkHttpWebSocket, response: OkHttpResponse )
            extends Event

        case class Message( payload: Either[ByteString, String] ) extends Event

        case class Failure( throwable: Throwable, response: OkHttpResponse )
            extends Event

        case class Closing( code: Int, reason: Option[String] ) extends Event

        case class Closed( code: Int, reason: Option[String] ) extends Event
    }

    def apply(
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        failureReconnect:  Option[FiniteDuration]              = Default.failureReconnect,
        completeReconnect: Option[FiniteDuration]              = Default.completeReconnect
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create( strategy ) { downstream ⇒
        import downstream.scheduler

        val cancelable = MultiAssignmentCancelable()

        def next( event: Event ): Unit = {
            event match {
                case Event.Failure( throwable, _ ) ⇒
                    logger.debug( s"Event: $event", throwable )
                case event ⇒ logger.debug( s"Event: $event" )
            }

            if ( downstream.onNext( event ) == Stop ) {
                cancelable.cancel()
            }
        }

        def reconnect( delay: FiniteDuration ): Cancelable = {
            logger.debug( s"Attempting reconnect in $delay" )

            cancelable := Task
                .delay {
                    next( Event.Reconnecting )
                    ohc.newWebSocket( request, listener )
                }
                .delayExecution( delay )
                .materialize
                .foreach {
                    case Success( socket ) ⇒
                        cancelable := cancellation( socket )
                        ()
                    case Failure( _ ) ⇒
                        cancelable := reconnect( delay )
                        ()
                }
        }

        lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
            override def onOpen(
                socket:   OkHttpWebSocket,
                response: OkHttpResponse
            ): Unit = next( Event.Open( socket, response ) )

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: String
            ): Unit = next( Event.Message( Right( message ) ) )

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: ByteString
            ): Unit = next( Event.Message( Left( message ) ) )

            override def onFailure(
                socket:    OkHttpWebSocket,
                exception: Throwable,
                response:  OkHttpResponse
            ): Unit = {
                next( Event.Failure( exception, response ) )

                failureReconnect match {
                    case Some( _ ) if cancelable.isCanceled ⇒
                        logger.debug {
                            "Not attempting to reconnect because the " +
                                "Observable has been cancelled explicitly"
                        }

                        downstream.onError( exception )
                    case Some( delay ) ⇒
                        cancelable := reconnect( delay )
                        ()
                    case None ⇒ downstream.onError( exception )
                }
            }

            override def onClosing(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                next {
                    Event.Closing(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                completeReconnect match {
                    case Some( _ ) if cancelable.isCanceled ⇒
                        logger.debug {
                            "Not attempting to reconnect because the " +
                                "Observable has been cancelled explicitly"
                        }
                    case Some( delay ) ⇒
                        cancelable := reconnect( delay )
                        ()
                    case None ⇒ close( socket )
                }
            }

            override def onClosed(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                next {
                    Event.Closed(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                val complete = completeReconnect.isEmpty ||
                    ( completeReconnect.isDefined && cancelable.isCanceled )

                if ( complete ) {
                    downstream.onComplete()
                }
            }
        }

        next( Event.Connecting )

        val socket = ohc.newWebSocket( request, listener )
        cancelable := cancellation( socket )
    }

    private def close( socket: OkHttpWebSocket ): Unit = {
        if ( socket.close( 1000, null ) ) {
            logger.debug( "Closing WebSocket connection" )
        }
    }

    private def cancellation( socket: OkHttpWebSocket ): Cancelable =
        Cancelable( () ⇒ close( socket ) )
}
