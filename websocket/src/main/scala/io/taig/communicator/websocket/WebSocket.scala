package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator._
import io.taig.communicator.websocket.WebSocket.Event
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3._
import okhttp3.ws.{ WebSocketCall, WebSocketListener }
import okio.Buffer

import scala.util.{ Failure, Success }

class WebSocket[T: Codec] {
    private var socket: OkHttpWebSocket = null

    private val buffer = collection.mutable.ListBuffer[Event[T]]()

    private def inject( socket: OkHttpWebSocket )( implicit c: Codec[T] ): Unit = {
        this.socket = socket

        buffer.foreach {
            case Event.Send( value )         ⇒ send( value )
            case Event.Ping( value )         ⇒ ping( value )
            case Event.Close( code, reason ) ⇒ close( code, reason )
        }
    }

    def send( value: T ): Unit = {
        if ( socket == null ) {
            buffer += Event.Send( value )
        } else {
            socket.sendMessage( Codec[T].encode( value ) )
        }
    }

    def ping( value: Option[T] = None ): Unit = {
        if ( socket == null ) {
            buffer += Event.Ping( value )
        } else {
            val sink = value.map { value ⇒
                val sink = new Buffer
                val request = Codec[T].encode( value )

                try {
                    request.writeTo( sink )
                    sink
                } finally {
                    sink.close()
                }
            }

            socket.sendPing( sink.orNull )
        }
    }

    def close( code: Int, reason: String ): Unit = {
        if ( socket == null ) {
            buffer += Event.Close( code, reason )
        } else {
            socket.close( code, reason )
        }
    }
}

object WebSocket {
    private sealed trait Event[+T]

    private object Event {
        case class Send[T]( value: T ) extends Event[T]
        case class Ping[T]( value: Option[T] ) extends Event[T]
        case class Close( code: Int, reason: String ) extends Event[Nothing]
    }

    def apply[T]( request: Request, strategy: OverflowStrategy.Synchronous[T] )(
        implicit
        cl: Client,
        co: Codec[T]
    ): ( WebSocket[T], Observable[T] ) = {
        val call = WebSocketCall.create( cl, request )

        val buffer = new WebSocket[T]
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

        ( buffer, observable )
    }
}