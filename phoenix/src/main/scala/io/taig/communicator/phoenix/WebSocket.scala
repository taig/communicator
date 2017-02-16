package io.taig.communicator.phoenix

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.{ Cancelable, Scheduler }
import monix.execution.cancelables.SerialCancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okio.ByteString

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object WebSocket {
    def apply(
        request:           OkHttpRequest,
        failureReconnect:  Option[FiniteDuration] = None,
        completeReconnect: Option[FiniteDuration] = None
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create( OverflowStrategy.Unbounded ) { downstream ⇒
        val sc = SerialCancelable()

        import downstream.scheduler

        lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
            override def onOpen(
                socket:   OkHttpWebSocket,
                response: OkHttpResponse
            ): Unit = {
                logger.debug( "WebSocket: Open" )

                if ( downstream.onNext( Event.Open( socket, response ) ) == Stop ) {
                    logger.info( "#1 onNext returned stop, cancelling" )
                    sc.cancel()
                }
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: String
            ): Unit = {
                logger.debug( "WebSocket: String-Message" )
                logger.debug( message )

                if ( downstream.onNext( Event.Message( Right( message ) ) ) == Stop ) {
                    logger.info( "#2 onNext returned stop, cancelling" )
                    sc.cancel()
                }
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: ByteString
            ): Unit = {
                logger.debug( "WebSocket: Byte-Message" )

                if ( downstream.onNext( Event.Message( Left( message ) ) ) == Stop ) {
                    logger.info( "#3 onNext returned stop, cancelling" )
                    sc.cancel()
                }
            }

            override def onFailure(
                socket:    OkHttpWebSocket,
                exception: Throwable,
                response:  OkHttpResponse
            ): Unit = {
                logger.debug( "WebSocket: Failure", exception )

                if ( downstream.onNext( Event.Failure( exception, response ) ) == Stop ) {
                    logger.info( "#4 onNext returned stop, cancelling" )
                    sc.cancel()
                }

                if ( !sc.isCanceled ) {
                    failureReconnect.fold( downstream.onError( exception ) ) {
                        delay ⇒
                            logger.debug( s"Initiating reconnect in $delay" )
                            sc := reconnect( request, listener, sc, delay )
                            ()
                    }
                } else {
                    logger.debug {
                        "Not attempting to reconnect because the " +
                            "Observable has been cancelled"
                    }

                    downstream.onError( exception )
                }
            }

            override def onClosing(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                logger.debug( s"WebSocket: Closing ($code)" )

                val ack = downstream.onNext {
                    Event.Closing(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                if ( ack == Stop ) {
                    logger.info( "#5 onNext returned stop, cancelling" )
                    sc.cancel()
                }
            }

            override def onClosed(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                logger.debug( s"WebSocket: Closed ($code)" )

                val ack = downstream.onNext {
                    Event.Closed(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                if ( ack == Stop ) {
                    logger.info( "#5 onNext returned stop, cancelling" )
                    sc.cancel()
                }

                if ( !sc.isCanceled ) {
                    completeReconnect.fold( downstream.onComplete() ) { delay ⇒
                        logger.debug( s"Initiating reconnect in $delay" )
                        sc := reconnect( request, listener, sc, delay )
                        ()
                    }
                } else {
                    logger.debug {
                        "Not attempting to reconnect because the " +
                            "Observable has been cancelled"
                    }

                    downstream.onComplete()
                }
            }
        }

        val socket = ohc.newWebSocket( request, listener )
        sc := cancel( socket )
    }

    private def reconnect(
        request:  OkHttpRequest,
        listener: OkHttpWebSocketListener,
        sc:       SerialCancelable,
        delay:    FiniteDuration
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Cancelable = {
        Task
            .delay( ohc.newWebSocket( request, listener ) )
            .delayExecution( delay )
            .materialize
            .foreach {
                case Success( socket ) ⇒
                    sc := cancel( socket )
                    ()
                case Failure( _ ) ⇒
                    sc := reconnect( request, listener, sc, delay )
                    ()
            }
    }

    private def cancel( socket: OkHttpWebSocket ): Cancelable =
        Cancelable { () ⇒
            socket.close( 1000, null )
            ()
        }

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