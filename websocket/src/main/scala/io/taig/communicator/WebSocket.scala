package io.taig.communicator

import java.io.IOException

import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3._
import okhttp3.ws.{ WebSocketCall, WebSocketListener, WebSocket ⇒ OkHttpSocket }
import okio.Buffer

object WebSocket {
    def apply( request: Request, strategy: OverflowStrategy.Synchronous[Array[Byte]] )(
        implicit
        c: Client,
        s: Scheduler
    ): Task[( OkHttpSocket, Observable[Array[Byte]] )] = {
        var socket: OkHttpSocket = null

        def close(): Unit = {
            if ( socket != null ) {
                socket.close( 1000, "" )
            }
        }

        Task.create[( OkHttpSocket, Observable[Array[Byte]] )] { ( _, callback ) ⇒
            val call = WebSocketCall.create( c, request )

            lazy val observable: Observable[Array[Byte]] = Observable.create( strategy ) { downstream ⇒
                def handle( data: Array[Byte] ): Unit = {
                    if ( downstream.onNext( data ) == Stop ) {
                        close()
                    }
                }

                call.enqueue {
                    new WebSocketListener {
                        override def onOpen( webSocket: OkHttpSocket, response: Response ) = {
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
}