package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator.OkHttpRequest
import monix.eval.{ Callback, Task }
import monix.execution.Cancelable
import monix.execution.atomic.AtomicBoolean
import okhttp3.ws.WebSocketCall
import okhttp3.{ OkHttpClient, Response, ResponseBody }
import okio.Buffer

import scala.util.{ Failure, Success }

trait WebSocket[T] {
    private[websocket] def raw: OkHttpWebSocket

    def send( value: T )( implicit e: Encoder[T] ): Unit

    def ping( value: Option[T] = None )( implicit e: Encoder[T] ): Unit

    def close( code: Int, reason: Option[String] ): Unit

    def isClosed: Boolean
}

object WebSocket {
    def apply[T: Decoder]( request: OkHttpRequest )(
        listener: WebSocket[T] ⇒ WebSocketListener[T]
    )(
        implicit
        c: OkHttpClient
    ): Task[( WebSocket[T], Option[T] )] = Task.create { ( _, callback ) ⇒
        val call = WebSocketCall.create( c, request )
        call.enqueue( new WebSocketListenerProxy( callback, listener ) )
        Cancelable( call.cancel )
    }
}

abstract class WebSocketListener[T]( val socket: WebSocket[T] ) {
    def onMessage( message: T ): Unit

    def onPong( payload: Option[T] ): Unit

    def onClose( code: Int, reason: Option[String] ): Unit

    def onFailure( exception: IOException, response: Option[T] ): Unit
}

class SimpleWebSocketListener[T]( socket: WebSocket[T] )
        extends WebSocketListener[T]( socket ) {
    override def onMessage( message: T ) = {}

    override def onPong( payload: Option[T] ) = {}

    override def onClose( code: Int, reason: Option[String] ) = {}

    override def onFailure( exception: IOException, response: Option[T] ) = {}
}

private class WebSocketListenerProxy[T: Decoder](
        callback: Callback[( WebSocket[T], Option[T] )],
        f:        WebSocket[T] ⇒ WebSocketListener[T]
) extends OkHttpWebSocketListener {
    var listener: WebSocketListener[T] = _

    override def onOpen( socket: OkHttpWebSocket, response: Response ) = synchronized {
        val wrapped = new OkHttpWebSocketWrapper[T]( socket )
        listener = f( wrapped )

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

        callback.onSuccess( ( wrapped, message ) )
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

private class OkHttpWebSocketWrapper[T]( socket: OkHttpWebSocket )
        extends WebSocket[T] {
    var closed = false

    override private[websocket] val raw = socket

    override def send( value: T )( implicit e: Encoder[T] ) = synchronized {
        if ( !closed ) {
            logger.debug {
                s"""
                   |Sending message
                   |  Payload: $value
                """.stripMargin.trim
            }

            socket.sendMessage( e.encode( value ) )
        } else {
            logger.warn( "Trying to send message on a closed socket" )
        }
    }

    override def ping( value: Option[T] )( implicit e: Encoder[T] ) = synchronized {
        if ( !closed ) {
            logger.debug {
                s"""
                   |Sending ping
                   |  Payload: ${value.orNull}
                """.stripMargin.trim
            }

            socket.sendPing( value.map( e.buffer ).orNull )
        } else {
            logger.warn( "Trying to send ping on a closed socket" )
        }
    }

    override def close( code: Int, reason: Option[String] ) = synchronized {
        if ( !closed ) {
            logger.debug {
                s"""
                   |Sending close
                   |  Code:   $code
                   |  Reason: ${reason.orNull}
                """.stripMargin.trim
            }

            closed = true
            socket.close( code, reason.orNull )
        }
    }

    override def isClosed = synchronized( closed )
}