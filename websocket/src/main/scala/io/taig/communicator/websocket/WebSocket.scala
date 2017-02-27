package io.taig.communicator.websocket

import java.security.SecureRandom

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.eval.Task
import monix.execution.Ack.{ Continue, Stop }
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString

import scala.concurrent.duration.FiniteDuration

object WebSocket {
    sealed trait Event

    object Event {
        case object Connecting extends Event

        case object Reconnecting extends Event

        case class Open( socket: OkHttpWebSocket ) extends Event

        case class Message( payload: Either[ByteString, String] ) extends Event

        case class Failure( throwable: Throwable ) extends Event

        case class Closing( code: Int, reason: Option[String] ) extends Event

        case class Closed( code: Int, reason: Option[String] ) extends Event
    }

    def apply(
        request:  OkHttpRequest,
        strategy: OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = WebSocket( request, strategy, Event.Connecting )

    private def apply(
        request:  OkHttpRequest,
        strategy: OverflowStrategy.Synchronous[Event],
        status:   Event
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.defer {
        var continue = true

        var socket: RealWebSocket = null

        def close(): Unit = if ( continue ) {
            continue = false

            if ( socket.close( 1000, null ) ) {
                logger.debug( "Closing WebSocket connection" )
            } else {
                logger.debug( "WebSocket already closed" )
            }
        }

        def cancel(): Unit = if ( continue ) {
            continue = false

            logger.debug( "Cancelling WebSocket connection" )
            socket.cancel()
        }

        Observable.create( strategy ) { downstream ⇒
            lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
                override def onOpen(
                    socket:   OkHttpWebSocket,
                    response: OkHttpResponse
                ): Unit = {
                    Option( response ).foreach( _.close() )

                    val event = Event.Open( socket )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )

                        if ( downstream.onNext( event ) == Stop ) {
                            close()
                        }
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }
                }

                override def onMessage(
                    socket:  OkHttpWebSocket,
                    message: String
                ): Unit = {
                    val event = Event.Message( Right( message ) )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )

                        if ( downstream.onNext( event ) == Stop ) {
                            close()
                        }
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }
                }

                override def onMessage(
                    socket:  OkHttpWebSocket,
                    message: ByteString
                ): Unit = {
                    val event = Event.Message( Left( message ) )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )

                        if ( downstream.onNext( event ) == Stop ) {
                            close()
                        }
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }
                }

                override def onFailure(
                    socket:    OkHttpWebSocket,
                    throwable: Throwable,
                    response:  OkHttpResponse
                ): Unit = {
                    Option( response ).foreach( _.close )

                    val event = Event.Failure( throwable )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )
                        downstream.onNext( event )
                        cancel()
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }

                    downstream.onError( throwable )
                }

                override def onClosing(
                    socket: OkHttpWebSocket,
                    code:   Int,
                    reason: String
                ): Unit = {
                    val event = Event.Closing(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )
                        downstream.onNext( event )
                        ()
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }
                }

                override def onClosed(
                    socket: OkHttpWebSocket,
                    code:   Int,
                    reason: String
                ): Unit = {
                    val event = Event.Closed(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )
                        downstream.onNext( event )
                        ()
                    } else {
                        logger.debug( s"Discarding: $event" )
                    }

                    downstream.onComplete()
                }
            }

            socket = new RealWebSocket( request, listener, new SecureRandom )

            logger.debug( s"Propagating: $status" )

            if ( downstream.onNext( status ) == Continue ) {
                socket.connect( ohc )
                Cancelable { () ⇒
                    logger.debug( "Explicitly cancel Observable" )
                    close()
                }
            } else Cancelable( () ⇒ cancel() )
        }.doOnEarlyStop { () ⇒
            logger.debug( "Initiate early stop shutdown" )
            close()
        }
    }

    def fromRequest(
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        errorReconnect:    Int ⇒ Option[FiniteDuration]        = _ ⇒ Default.errorReconnect,
        completeReconnect: Int ⇒ Option[FiniteDuration]        = _ ⇒ Default.completeReconnect
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = fromTask(
        Task.now( request ),
        strategy,
        errorReconnect,
        completeReconnect
    )

    def fromTask(
        request:           Task[OkHttpRequest],
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        errorReconnect:    Int ⇒ Option[FiniteDuration]        = _ ⇒ Default.errorReconnect,
        completeReconnect: Int ⇒ Option[FiniteDuration]        = _ ⇒ Default.completeReconnect
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = fromTaskCounting(
        request,
        strategy,
        errorReconnect,
        completeReconnect
    )

    private def fromTaskCounting(
        request:           Task[OkHttpRequest],
        strategy:          OverflowStrategy.Synchronous[Event],
        errorReconnect:    Int ⇒ Option[FiniteDuration],
        completeReconnect: Int ⇒ Option[FiniteDuration],
        retries:           Int                                 = 1,
        status:            Event                               = Event.Connecting
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = {
        var counter = retries

        Observable.fromTask( request ).mergeMap { r ⇒
            WebSocket( r, strategy, status ).mergeMapDelayErrors {
                case Event.Failure( throwable ) ⇒
                    Observable.raiseError( throwable )
                case Event.Closed( _, _ ) ⇒ completeReconnect( retries ) match {
                    case Some( delay ) ⇒
                        fromTaskCounting(
                            request,
                            strategy,
                            errorReconnect,
                            completeReconnect,
                            counter + 1,
                            Event.Reconnecting
                        ).delaySubscription( delay )
                    case None ⇒ Observable.empty
                }
                case event ⇒ Observable.now( event )
            }
        }.onErrorHandleWith { throwable ⇒
            errorReconnect( retries ) match {
                case Some( delay ) ⇒
                    fromTaskCounting(
                        request,
                        strategy,
                        errorReconnect,
                        completeReconnect,
                        counter + 1,
                        Event.Reconnecting
                    ).delaySubscription( delay )
                case None ⇒ Observable.raiseError( throwable )
            }
        }.doOnNext {
            case Event.Open( _ ) ⇒ counter = 1
            case _               ⇒ //
        }
    }
}
