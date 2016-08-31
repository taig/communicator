package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator.OkHttpRequest
import monix.eval.{ Callback, Task }
import monix.execution.Cancelable
import okhttp3.ws.WebSocketCall
import okhttp3.{ OkHttpClient, Response, ResponseBody }
import okio.Buffer

import scala.util.{ Failure, Success }

object WebSocket {
    def apply[T: Decoder]( request: OkHttpRequest )(
        listener: OkHttpWebSocket ⇒ WebSocketListener[T]
    )(
        implicit
        c: OkHttpClient
    ): Task[( OkHttpWebSocket, Option[T] )] = Task.create { ( _, callback ) ⇒
        val call = WebSocketCall.create( c, request )
        call.enqueue( new WebSocketListenerProxy( callback, listener ) )
        Cancelable( call.cancel )
    }
}

abstract class WebSocketListener[T]( val socket: OkHttpWebSocket ) {
    def onMessage( message: T ): Unit

    def onPong( payload: Option[T] ): Unit

    def onClose( code: Int, reason: Option[String] ): Unit

    def onFailure( exception: IOException, response: Option[T] ): Unit
}

private class WebSocketListenerProxy[T: Decoder](
        callback: Callback[( OkHttpWebSocket, Option[T] )],
        f:        OkHttpWebSocket ⇒ WebSocketListener[T]
) extends OkHttpWebSocketListener {
    var listener: WebSocketListener[T] = _

    override def onOpen( socket: OkHttpWebSocket, response: Response ) = synchronized {
        listener = f( socket )

        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Decoder[T].decode( _ ).toOption )

        logger.debug {
            s"""
               |onOpen
               |  Payload: ${message.orNull}
            """.stripMargin.trim
        }

        callback.onSuccess( ( socket, message ) )
    }

    override def onFailure( exception: IOException, response: Response ) = synchronized {
        val message = Option( response )
            .flatMap( response ⇒ Option( response.body() ) )
            .map( _.bytes() )
            .flatMap( Decoder[T].decode( _ ).toOption )

        logger.debug( {
            s"""
               |onFailure
               |  Payload: $message
            """.stripMargin.trim
        }, exception )

        if ( listener == null ) {
            callback.onError( exception )
        } else {
            listener.onFailure( exception, message )
        }
    }

    override def onMessage( response: ResponseBody ) = {
        val bytes = response.bytes()

        Decoder[T].decode( bytes ) match {
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
            .flatMap( Decoder[T].decode( _ ).toOption )

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