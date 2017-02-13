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
        strategy:          OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        failureReconnect:  Option[FiniteDuration]              = None,
        completeReconnect: Option[FiniteDuration]              = None
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create( strategy ) { downstream ⇒
        val sc = SerialCancelable()

        import downstream.scheduler

        lazy val listener: OkHttpWebSocketListener = new OkHttpWebSocketListener {
            override def onOpen(
                socket:   OkHttpWebSocket,
                response: OkHttpResponse
            ): Unit = {
                downstream.onNext( Event.Open( socket, response ) )
                ()
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: String
            ): Unit = {
                val ack = downstream.onNext( Event.Message( Right( message ) ) )

                if ( ack == Stop ) {
                    socket.cancel()
                }
            }

            override def onMessage(
                socket:  OkHttpWebSocket,
                message: ByteString
            ): Unit = {
                val ack = downstream.onNext( Event.Message( Left( message ) ) )

                if ( ack == Stop ) {
                    socket.cancel()
                }
            }

            override def onFailure(
                socket:    OkHttpWebSocket,
                exception: Throwable,
                response:  OkHttpResponse
            ): Unit = {
                downstream.onNext( Event.Failure( exception, response ) )

                failureReconnect.fold( downstream.onError( exception ) ) {
                    delay ⇒
                        sc := reconnect( request, listener, sc, delay )
                        ()
                }

                ()
            }

            override def onClosing(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                downstream.onNext {
                    Event.Closing(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                ()
            }

            override def onClosed(
                socket: OkHttpWebSocket,
                code:   Int,
                reason: String
            ): Unit = {
                downstream.onNext {
                    Event.Closed(
                        code,
                        Some( reason ).filter( _.nonEmpty )
                    )
                }

                completeReconnect.fold( downstream.onComplete() ) { delay ⇒
                    sc := reconnect( request, listener, sc, delay )
                    ()
                }

                ()
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
    ): Cancelable =
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