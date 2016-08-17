package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator.OkHttpRequest
import monix.eval.{ Callback, Task }
import monix.execution.Cancelable
import monix.execution.atomic.AtomicBoolean
import okhttp3.{ HttpUrl, OkHttpClient, Response, ResponseBody }
import okhttp3.ws.WebSocketCall
import okio.Buffer

import scala.util.{ Failure, Success }

object WebSocket {
    def apply[T: Codec]( request: OkHttpRequest )( listener: WebSocketListener[T] )(
        implicit
        client: OkHttpClient
    ): Task[( OkHttpWebSocket, Option[T] )] = {
        Task.create { ( _, callback ) ⇒
            val call = WebSocketCall.create( client, request )

            call.enqueue(
                new WebSocketListenerProxy(
                    request.url(),
                    callback,
                    listener
                )
            )

            Cancelable { () ⇒ call.cancel() }
        }
    }

    def pure[T: Codec]( request: OkHttpRequest )(
        implicit
        client: OkHttpClient
    ): Task[( OkHttpWebSocket, Option[T] )] = {
        WebSocket( request )( new WebSocketListenerNoop[T] )
    }
}

trait WebSocketListener[T] {
    def onMessage( message: T ): Unit

    def onPong( payload: Option[T] ): Unit

    def onClose( code: Int, reason: Option[String] ): Unit

    def onFailure( exception: IOException, response: Option[T] ): Unit
}

private class WebSocketListenerNoop[T] extends WebSocketListener[T] {
    override def onMessage( message: T ) = {}

    override def onPong( payload: Option[T] ) = {}

    override def onClose( code: Int, reason: Option[String] ) = {}

    override def onFailure( exception: IOException, response: Option[T] ) = {}
}

private class WebSocketListenerProxy[T: Codec](
        url:      HttpUrl,
        callback: Callback[( OkHttpWebSocket, Option[T] )],
        listener: WebSocketListener[T]
) extends OkHttpWebSocketListener {
    val initialized = AtomicBoolean( false )

    override def onOpen( socket: OkHttpWebSocket, response: Response ) = {
        initialized.set( true )

        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Codec[T].decode( _ ).toOption )

        logger.debug {
            s"""
               |[$url] onOpen
               |  Payload: ${message.orNull}
            """.stripMargin.trim
        }

        callback.onSuccess( ( socket, message ) )
    }

    override def onFailure( exception: IOException, response: Response ) = {
        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Codec[T].decode( _ ).toOption )

        logger.debug( {
            s"""
               |[$url] onFailure
               |  Payload: $message
            """.stripMargin.trim
        }, exception )

        if ( initialized.compareAndSet( false, true ) ) {
            callback.onError( exception )
        } else {
            listener.onFailure( exception, message )
        }
    }

    override def onMessage( response: ResponseBody ) = {
        Codec[T].decode( response.bytes() ) match {
            case Success( message ) ⇒
                logger.debug {
                    s"""
                       |[$url] onMessage
                       |  Payload: $message
                    """.stripMargin
                }

                listener.onMessage( message )
            case Failure( exception ) ⇒
                listener.onFailure(
                    new IOException( "Failed to parse message", exception ),
                    None
                )
        }
    }

    override def onPong( payload: Buffer ) = {
        val message = Option( payload )
            .map( _.readByteArray() )
            .flatMap( Codec[T].decode( _ ).toOption )

        logger.debug {
            s"""
               |[$url] onPing
               |  Payload: ${message.orNull}
            """.stripMargin.trim
        }

        listener.onPong( message )
    }

    override def onClose( code: Int, reason: String ) = {
        val optionalReason = Some( reason ).filter( _.nonEmpty )

        logger.debug {
            s"""
               |[$url] onClose
               |  Code:   $code
               |  Reason: ${optionalReason.orNull}
            """.stripMargin.trim
        }

        listener.onClose( code, optionalReason )
    }
}