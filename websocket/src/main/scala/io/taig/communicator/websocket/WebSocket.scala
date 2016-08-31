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
    def apply[T: Decoder]( request: OkHttpRequest )( listener: WebSocketListener[T] )(
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
}

trait WebSocketListener[T] {
    def onMessage( message: T ): Unit

    def onPong( payload: Option[T] ): Unit

    def onClose( code: Int, reason: Option[String] ): Unit

    def onFailure( exception: IOException, response: Option[T] ): Unit
}

private class WebSocketListenerProxy[I: Decoder](
        url:      HttpUrl,
        callback: Callback[( OkHttpWebSocket, Option[I] )],
        listener: WebSocketListener[I]
) extends OkHttpWebSocketListener {
    val initialized = AtomicBoolean( false )

    override def onOpen( socket: OkHttpWebSocket, response: Response ) = {
        initialized.set( true )

        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Decoder[I].decode( _ ).toOption )

        logger.debug {
            s"""
               |onOpen
               |  Payload: ${message.orNull}
            """.stripMargin.trim
        }

        callback.onSuccess( ( socket, message ) )
    }

    override def onFailure( exception: IOException, response: Response ) = {
        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Decoder[I].decode( _ ).toOption )

        logger.debug( {
            s"""
               |onFailure
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
        val bytes = response.bytes()

        Decoder[I].decode( bytes ) match {
            case Success( message ) ⇒
                logger.debug {
                    s"""
                       |onMessage
                       |  Payload: $message
                    """.stripMargin.trim
                }

                listener.onMessage( message )
            case Failure( exception ) ⇒
                logger.error( {
                    s"""
                      |Failed to parse message
                      |  Payload: ${new String( bytes )}
                    """.stripMargin.trim
                }, exception )
        }
    }

    override def onPong( payload: Buffer ) = {
        val message = Option( payload )
            .map( _.readByteArray() )
            .flatMap( Decoder[I].decode( _ ).toOption )

        logger.debug {
            s"""
               |onPing
               |  Payload: ${message.orNull}
            """.stripMargin.trim
        }

        listener.onPong( message )
    }

    override def onClose( code: Int, reason: String ) = {
        val optionalReason = Some( reason ).filter( _.nonEmpty )

        logger.debug {
            s"""
               |onClose
               |  Code:   $code
               |  Reason: ${optionalReason.orNull}
            """.stripMargin.trim
        }

        listener.onClose( code, optionalReason )
    }
}