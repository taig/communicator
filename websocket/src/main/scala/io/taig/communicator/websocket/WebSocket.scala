package io.taig.communicator.websocket

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.{ Cancelable, Scheduler }
import monix.execution.cancelables.MultiAssignmentCancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okio.ByteString

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object WebSocket {
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

        lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
            override def onOpen(
                socket:   OkHttpWebSocket,
                response: OkHttpResponse
            ): Unit = {
                logger.debug( "Open" )

                val ack = downstream.onNext( Event.Open( socket, response ) )

                if ( ack == Stop ) {
                    cancelable.cancel()
                }
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: String
            ): Unit = {
                logger.debug( s"String-Message: $message" )

                val ack = downstream.onNext( Event.Message( Right( message ) ) )

                if ( ack == Stop ) {
                    cancelable.cancel()
                }
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: ByteString
            ): Unit = {
                logger.debug( "Byte-Message" )

                val ack = downstream.onNext( Event.Message( Left( message ) ) )

                if ( ack == Stop ) {
                    cancelable.cancel()
                }
            }

            override def onFailure(
                socket:    OkHttpWebSocket,
                exception: Throwable,
                response:  OkHttpResponse
            ): Unit = {
                logger.debug( "Failure", exception )

                val ack = downstream
                    .onNext( Event.Failure( exception, response ) )

                if ( ack == Stop ) {
                    cancelable.cancel()
                }

                failureReconnect match {
                    case Some( _ ) if cancelable.isCanceled ⇒
                        logger.debug {
                            "Not attempting to reconnect because the " +
                                "Observable has been cancelled"
                        }

                        downstream.onError( exception )
                    case Some( delay ) ⇒
                        cancelable := reconnect(
                            request,
                            listener,
                            cancelable,
                            delay
                        )
                        ()
                    case None ⇒ downstream.onError( exception )
                }
            }

            override def onClosing(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                logger.debug( s"Closing ($code)" )

                val ack = downstream.onNext {
                    Event.Closing(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                if ( ack == Stop ) {
                    cancelable.cancel()
                }

                completeReconnect match {
                    case Some( _ ) if cancelable.isCanceled ⇒
                        logger.debug {
                            "Not attempting to reconnect because the " +
                                "Observable has been cancelled"
                        }
                    case Some( delay ) ⇒
                        cancelable := reconnect(
                            request,
                            listener,
                            cancelable,
                            delay
                        )
                        ()
                    case None ⇒ close( socket )
                }
            }

            override def onClosed(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                logger.debug( s"Closed ($code)" )

                val ack = downstream.onNext {
                    Event.Closed(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                if ( ack == Stop ) {
                    cancelable.cancel()
                }

                val complete = completeReconnect.isEmpty ||
                    ( completeReconnect.isDefined && cancelable.isCanceled )

                if ( complete ) {
                    downstream.onComplete()
                }
            }
        }

        val socket = ohc.newWebSocket( request, listener )
        cancelable := cancellation( socket )
    }

    private def reconnect(
        request:    OkHttpRequest,
        listener:   OkHttpWebSocketListener,
        cancelable: MultiAssignmentCancelable,
        delay:      FiniteDuration
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Cancelable = {
        logger.debug( s"Initiating reconnect in $delay" )

        Task
            .delay( ohc.newWebSocket( request, listener ) )
            .delayExecution( delay )
            .materialize
            .foreach {
                case Success( socket ) ⇒
                    cancelable := cancellation( socket )
                    ()
                case Failure( _ ) ⇒
                    cancelable := reconnect( request, listener, cancelable, delay )
                    ()
            }
    }

    private def close( socket: OkHttpWebSocket ): Unit = {
        if ( socket.close( 1000, null ) ) {
            logger.debug( "Closing WebSocket connection" )
        }
    }

    private def cancellation( socket: OkHttpWebSocket ): Cancelable =
        Cancelable( () ⇒ close( socket ) )

    sealed trait Event

    object Event {
        case class Open( socket: OkHttpWebSocket, response: OkHttpResponse )
            extends Event

        case class Message( payload: Either[ByteString, String] ) extends Event

        case class Failure( exception: Throwable, response: OkHttpResponse )
            extends Event

        case class Closing( code: Int, reason: Option[String] ) extends Event

        case class Closed( code: Int, reason: Option[String] ) extends Event
    }
}
