package io.taig.communicator

import java.io.IOException

import monix.eval.{ Callback, Task }
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.execution.atomic.AtomicBoolean
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3._
import okhttp3.ws.{ WebSocketCall, WebSocketListener, WebSocket ⇒ OkHttpSocket }
import okio.Buffer

private case class Listener(
        callback: Callback[( OkHttpSocket, Listener )]
) extends WebSocketListener {
    val open = AtomicBoolean( false )

    var onMessage: Array[Byte] ⇒ Unit = null

    var onClose: () ⇒ Unit = null

    var onFailure: Throwable ⇒ Unit = null

    override def onOpen( socket: OkHttpSocket, response: Response ) = {
        if ( open.compareAndSet( false, true ) ) {
            callback.onSuccess( socket, this )
        } else {
            throw new IllegalStateException( "Socket already open" )
        }
    }

    override def onMessage( message: ResponseBody ) = onMessage( message.bytes() )

    override def onPong( payload: Buffer ) = ???

    override def onClose( code: Int, reason: String ) = onClose()

    override def onFailure( exception: IOException, response: Response ) = {
        if ( open.compareAndSet( false, true ) ) {
            callback.onError( exception )
        } else {
            onFailure( exception )
        }
    }
}

object WebSocket {
    def apply( request: Request, strategy: OverflowStrategy.Synchronous[Array[Byte]] )(
        implicit
        c: Client
    ): Task[( OkHttpSocket, Observable[Array[Byte]] )] = {
        Task.create[( OkHttpSocket, Listener )] { ( _, callback ) ⇒
            val call = WebSocketCall.create( c, request )
            call.enqueue( Listener( callback ) )
            Cancelable { () ⇒ call.cancel() }
        } map {
            case ( socket, listener ) ⇒
                val observable = Observable.create( strategy ) { downstream ⇒
                    listener.onMessage = { data ⇒
                        if ( downstream.onNext( data ) == Stop ) {
                            socket.close( 1000, "" )
                        }
                    }

                    listener.onClose = () ⇒ downstream.onComplete()

                    listener.onFailure = downstream.onError

                    Cancelable { () ⇒ socket.close( 1000, "" ) }
                }

                ( socket, observable )
        }
    }
}