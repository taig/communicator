package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator._
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3._
import okhttp3.ws.{ WebSocketCall, WebSocketListener }
import okio.Buffer

import scala.util.{ Failure, Success }

trait WebSocket[S, +R] {
    def sender: WebSocket.Sender[S]

    def receiver: WebSocket.Receiver[R]

    def close(): Unit
}

object WebSocket {
    trait Sender[S] {
        def send( value: S ): Unit

        def ping( value: Option[S] = None ): Unit

        def close( code: Int, reason: String ): Unit
    }

    type Receiver[+R] = Observable[R]

    def apply[T]( request: Request, strategy: OverflowStrategy.Synchronous[T] )(
        implicit
        cl: Client,
        co: Codec[T]
    ): WebSocket[T, T] = {
        val call = WebSocketCall.create( cl, request )

        val buffer = new BufferedOkHttpWebSocket

        var socket: OkHttpWebSocket = null

        def close(): Unit = {
            if ( socket != null ) {
                socket.close( Close.GoingAway, "Client disconnected" )
            }
        }

        val observable = Observable.create( strategy ) { downstream ⇒
            def handle( data: Array[Byte] ): Unit = {
                co.decode( data ) match {
                    case Success( data ) ⇒
                        if ( downstream.onNext( data ) == Stop ) {
                            close()
                        }
                    case Failure( exception ) ⇒
                        downstream.onError( exception )

                }
            }

            call.enqueue {
                new WebSocketListener {
                    override def onOpen( websocket: OkHttpWebSocket, response: Response ) = {
                        socket = websocket
                        buffer.inject( websocket )
                    }

                    override def onMessage( response: ResponseBody ) = {
                        handle( response.bytes() )
                    }

                    override def onPong( payload: Buffer ) = {
                        handle( payload.readByteArray() )
                    }

                    override def onClose( code: Int, reason: String ) = {
                        downstream.onComplete()
                    }

                    override def onFailure( exception: IOException, response: Response ) = {
                        downstream.onError( exception )
                    }
                }
            }

            Cancelable( close )
        }

        new WebSocket[T, T] {
            override val sender = new BufferedWebSocketSender[T]( buffer )

            override val receiver = observable

            override def close() = {
                sender.close( Close.GoingAway, "Bye." )
            }
        }
    }

    def unapply[S, R](
        websocket: WebSocket[S, R]
    ): Option[( Sender[S], Receiver[R] )] = {
        Some( websocket.sender, websocket.receiver )
    }
}

