package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator._
import okhttp3.ws.WebSocket.TEXT
import monix.eval.Task
import monix.execution.Ack.{ Continue, Stop }
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, Observer, OverflowStrategy }
import okhttp3._
import okhttp3.ws.{ WebSocketCall, WebSocketListener }
import okio.Buffer

import scala.util.{ Failure, Success }

object WebSocket {
    def apply( request: Request, strategy: OverflowStrategy.Synchronous[Array[Byte]] )(
        implicit
        c: Client,
        s: Scheduler
    ): Task[( OkHttpWebSocket, Observable[Array[Byte]] )] = {
        var socket: OkHttpWebSocket = null

        def close(): Unit = {
            if ( socket != null ) {
                socket.close( Close.GoingAway, "Client disconnected" )
            }
        }

        Task.create[( OkHttpWebSocket, Observable[Array[Byte]] )] { ( _, callback ) ⇒
            val call = WebSocketCall.create( c, request )

            lazy val observable: Observable[Array[Byte]] = Observable.create( strategy ) { downstream ⇒
                def handle( data: Array[Byte] ): Unit = {
                    if ( downstream.onNext( data ) == Stop ) {
                        close()
                    }
                }

                call.enqueue {
                    new WebSocketListener {
                        override def onOpen( webSocket: OkHttpWebSocket, response: Response ) = {
                            socket = webSocket
                            callback.onSuccess( ( webSocket, observable ) )
                            Option( response.body() ).map( _.bytes() ).foreach( handle )
                        }

                        override def onMessage( message: ResponseBody ) = {
                            handle( message.bytes() )
                        }

                        override def onPong( payload: Buffer ) = {
                            Option( payload ).map( _.readByteArray() ).foreach( handle )
                        }

                        override def onClose( code: Int, reason: String ) = {
                            downstream.onComplete()
                        }

                        override def onFailure( exception: IOException, response: Response ) = {
                            if ( socket == null ) {
                                callback.onError( exception )
                            } else {
                                downstream.onError( exception )
                            }
                        }
                    }
                }

                Cancelable.empty
            }.share

            val subscription = observable.subscribe()

            Cancelable { () ⇒
                call.cancel()
                subscription.cancel()
                close()
            }
        }
    }

    def apply2[T]( request: Request, strategy: OverflowStrategy.Synchronous[T] )(
        implicit
        cl: Client,
        co: Codec[T]
    ): ( Observer[T], Observable[T] ) = {
        val call = WebSocketCall.create( cl, request )

        val buffer = collection.mutable.ListBuffer[Event[T]]()

        var socket: OkHttpWebSocket = null

        def close(): Unit = {
            if ( socket != null ) {
                socket.close( Close.GoingAway, "Client disconnected" )
            }
        }

        val observer = new Observer[T] {
            override def onNext( value: T ) = {
                if ( socket == null ) {
                    buffer += Event.OnNext( value )
                } else {
                    socket.sendMessage( co.encode( value ) )
                }

                Continue
            }

            override def onError( exception: Throwable ) = {
                if ( socket == null ) {
                    buffer += Event.OnError( exception )
                } else {
                    socket.close( Close.ProtocolError, exception.getMessage )
                }
            }

            override def onComplete() = {
                if ( socket == null ) {
                    buffer += Event.OnComplete
                } else {
                    socket.close( Close.Normal, "Bye" )
                }
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
                    override def onOpen( webSocket: OkHttpWebSocket, response: Response ) = {
                        socket = webSocket

                        buffer.foreach {
                            case Event.OnNext( value )      ⇒ observer.onNext( value )
                            case Event.OnError( exception ) ⇒ observer.onError( exception )
                            case Event.OnComplete           ⇒ observer.onComplete()
                        }
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

        ( observer, observable )
    }
}