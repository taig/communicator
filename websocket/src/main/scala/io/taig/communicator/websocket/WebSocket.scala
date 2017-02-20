package io.taig.communicator.websocket

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.execution.Ack.{ Continue, Stop }
import monix.execution.Cancelable
import monix.execution.cancelables.MultiAssignmentCancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
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
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        failureReconnect:  Option[FiniteDuration]              = Default.failureReconnect,
        completeReconnect: Option[FiniteDuration]              = Default.completeReconnect
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.defer {
        var socket: OkHttpWebSocket = null

        val reconnecting = MultiAssignmentCancelable()

        var continue = true

        def close(): Unit = {
            continue = false

            reconnecting.cancel()

            if ( socket != null ) {
                if ( socket.close( 1000, null ) ) {
                    logger.debug( "Closing WebSocket connection" )
                } else {
                    logger.debug( "WebSocket already closed" )
                }
            }
        }

        def cancel(): Unit = {
            continue = false

            reconnecting.cancel()

            if ( socket != null ) {
                logger.debug( "Cancelling WebSocket connection" )
                socket.cancel()
            }
        }

        Observable.create( strategy ) { downstream ⇒
            import downstream.scheduler

            def reconnect( delay: FiniteDuration ): Unit = {
                logger.debug( s"Attempting to reconnect in $delay" )

                if ( socket != null ) {
                    socket.cancel()
                }

                socket = null

                val timer = scheduler.scheduleOnce( delay ) {
                    val event = Event.Reconnecting

                    reconnecting := Cancelable.empty

                    if ( continue ) {
                        logger.debug( s"Propagating: $event" )

                        if ( downstream.onNext( event ) == Continue ) {
                            socket = ohc.newWebSocket( request, listener )
                        }
                    } else {
                        // No socket available, no further reconnect going on.
                        // There is nothing left to do.
                        logger.debug( s"Discarding: $event" )
                    }
                }

                reconnecting := Cancelable { () ⇒
                    logger.debug( "Cancelling reconnect attempt" )
                    timer.cancel()
                }

                ()
            }

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
                        close()
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

                    ( continue, failureReconnect ) match {
                        case ( true, Some( delay ) ) ⇒
                            logger.debug( s"Propagating: $event" )
                            downstream.onNext( event )
                            reconnect( delay )
                        case ( true, None ) ⇒
                            logger.debug( s"Propagating: $event" )
                            downstream.onNext( event )
                            downstream.onError( throwable )
                            cancel()
                        case ( false, delay ) ⇒
                            logger.debug( s"Discarding: $event" )

                            if ( delay.nonEmpty ) {
                                logger.debug {
                                    "Not attempting to reconnect because the " +
                                        "Observable has been stopped explicitly"
                                }
                            }

                            cancel()
                            downstream.onError( throwable )
                    }
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

                    ( continue, completeReconnect ) match {
                        case ( true, Some( delay ) ) ⇒
                            logger.debug( s"Propagating: $event" )
                            downstream.onNext( event )
                            reconnect( delay )
                        case ( true, None ) ⇒
                            logger.debug( s"Propagating: $event" )
                            downstream.onNext( event )
                            downstream.onComplete()
                            close()
                        case ( false, delay ) ⇒
                            logger.debug( s"Discarding: $event" )

                            if ( delay.nonEmpty ) {
                                logger.debug {
                                    "Not attempting to reconnect because the " +
                                        "Observable has been stopped explicitly"
                                }
                            }

                            close()
                            downstream.onComplete()
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
                }
            }

            logger.debug( s"Propagating: ${Event.Connecting}" )

            if ( downstream.onNext( Event.Connecting ) == Continue ) {
                socket = ohc.newWebSocket( request, listener )

                Cancelable { () ⇒
                    reconnecting.cancel()
                    close()
                }
            } else {
                Cancelable.empty
            }
        }.doOnEarlyStop { () ⇒
            logger.debug( "Early stop shutdown triggered" )
            close()
        }
    }
}
