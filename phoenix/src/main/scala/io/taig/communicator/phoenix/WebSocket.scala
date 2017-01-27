package io.taig.communicator.phoenix

import io.taig.communicator.{ OkHttpRequest, OkHttpResponse, OkHttpWebSocket, OkHttpWebSocketListener }
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import okio.ByteString

object WebSocket {
    def apply(
        request:  OkHttpRequest,
        strategy: OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create( strategy ) { downstream ⇒
        val listener = new OkHttpWebSocketListener {
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
                downstream.onError( exception )
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
                downstream.onComplete()
            }
        }

        val socket = ohc.newWebSocket( request, listener )

        Cancelable { () ⇒
            socket.close( 1000, null )
            ()
        }
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