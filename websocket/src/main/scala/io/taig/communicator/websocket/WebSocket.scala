package io.taig.communicator.websocket

import java.security.SecureRandom

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.execution.Ack.{ Continue, Stop }
import monix.execution.Cancelable
import monix.execution.cancelables.SerialCancelable
import monix.reactive.observers.Subscriber
import monix.reactive.{ Notification, Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

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

    private def apply(
        request:  OkHttpRequest,
        strategy: OverflowStrategy.Synchronous[Event],
        status:   Event
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.defer {
        val id = Random.nextInt( 10000 )
        logger.debug( s"Creating WebSocket ($id)" )

        var continue = true
        var socket: RealWebSocket = null

        def close(): Unit = if ( continue ) {
            continue = false

            if ( socket.close( 1000, null ) ) {
                logger.debug( s"Closing WebSocket connection ($id)" )
            } else {
                logger.debug( s"WebSocket already closed ($id)" )
            }
        }

        def cancel(): Unit = if ( continue ) {
            continue = false

            logger.debug( s"Cancelling WebSocket connection ($id)" )
            socket.cancel()
        }

        Observable.create( strategy ) { subscriber ⇒
            lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
                override def onOpen(
                    socket:   OkHttpWebSocket,
                    response: OkHttpResponse
                ): Unit = {
                    Option( response ).foreach( _.close() )

                    val event = Event.Open( socket )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event ($id)" )

                        if ( subscriber.onNext( event ) == Stop ) {
                            close()
                        }
                    } else logger.debug( s"Discarding: $event ($id)" )
                }

                override def onMessage(
                    socket:  OkHttpWebSocket,
                    message: String
                ): Unit = {
                    val event = Event.Message( Right( message ) )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event ($id)" )

                        if ( subscriber.onNext( event ) == Stop ) {
                            close()
                        }
                    } else logger.debug( s"Discarding: $event ($id)" )
                }

                override def onMessage(
                    socket:  OkHttpWebSocket,
                    message: ByteString
                ): Unit = {
                    val event = Event.Message( Left( message ) )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event ($id)" )

                        if ( subscriber.onNext( event ) == Stop ) {
                            close()
                        }
                    } else logger.debug( s"Discarding: $event ($id)" )
                }

                override def onFailure(
                    socket:    OkHttpWebSocket,
                    throwable: Throwable,
                    response:  OkHttpResponse
                ): Unit = {
                    Option( response ).foreach( _.close )

                    val event = Event.Failure( throwable )

                    if ( continue ) {
                        logger.debug( s"Propagating: $event ($id)" )
                        subscriber.onNext( event )
                        cancel()
                    } else logger.debug( s"Discarding: $event ($id)" )

                    subscriber.onError( throwable )
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
                        logger.debug( s"Propagating: $event ($id)" )
                        if ( subscriber.onNext( event ) == Stop ) {
                            continue = false
                        }
                    } else logger.debug( s"Discarding: $event ($id)" )
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
                        logger.debug( s"Propagating: $event ($id)" )
                        subscriber.onNext( event )
                        ()
                    } else logger.debug( s"Discarding: $event ($id)" )

                    subscriber.onComplete()
                }
            }

            socket = new RealWebSocket( request, listener, new SecureRandom )

            logger.debug( s"Propagating: $status ($id)" )

            if ( subscriber.onNext( status ) == Continue ) {
                socket.connect( ohc )
                Cancelable { () ⇒
                    logger.debug( s"Shutdown WebSocket Observable ($id)" )
                    close()
                }
            } else Cancelable( () ⇒ cancel() )
        }
    }

    def apply(
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        errorReconnect:    Int ⇒ Option[FiniteDuration]        = Default.errorReconnect,
        completeReconnect: Int ⇒ Option[FiniteDuration]        = Default.completeReconnect
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create( strategy ) { subscriber ⇒
        val cancelable = SerialCancelable()

        subscribe( subscriber )(
            request,
            strategy,
            errorReconnect,
            completeReconnect,
            Event.Connecting,
            1,
            cancelable
        )

        cancelable
    }

    private def subscribe(
        subscriber: Subscriber.Sync[Event]
    )(
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[Event],
        errorReconnect:    Int ⇒ Option[FiniteDuration],
        completeReconnect: Int ⇒ Option[FiniteDuration],
        status:            Event,
        retries:           Int,
        cancelable:        SerialCancelable
    )(
        implicit
        ohc: OkHttpClient
    ): Cancelable = {
        import subscriber.scheduler

        var counter = retries

        def reconnect( delay: FiniteDuration ): Unit = {
            cancelable := scheduler.scheduleOnce( delay ) {
                subscribe( subscriber )(
                    request,
                    strategy,
                    errorReconnect,
                    completeReconnect,
                    Event.Reconnecting,
                    counter + 1,
                    cancelable
                )
                ()
            }

            logger.debug( s"Attempting to reconnect in $delay (retry #$counter)" )
        }

        cancelable := WebSocket( request, strategy, status ).materialize.foreach {
            case Notification.OnNext( event ) ⇒
                if ( event.isInstanceOf[Event.Open] ) {
                    counter = 1
                }

                if ( subscriber.onNext( event ) == Stop ) {
                    cancelable.cancel()
                }
            case Notification.OnError( throwable ) ⇒
                errorReconnect( retries ) match {
                    case Some( delay ) if !cancelable.isCanceled ⇒ reconnect( delay )
                    case _                                       ⇒ subscriber.onError( throwable )
                }
            case Notification.OnComplete ⇒
                completeReconnect( retries ) match {
                    case Some( delay ) if !cancelable.isCanceled ⇒ reconnect( delay )
                    case _                                       ⇒ subscriber.onComplete()
                }
        }
    }
}
