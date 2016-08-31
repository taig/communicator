package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator.OkHttpRequest
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.observers.{ BufferedSubscriber, Subscriber }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.{ OkHttpClient, Response, ResponseBody }
import okhttp3.ws.WebSocketCall
import okio.Buffer

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

class WebSocketReader[T] private[websocket] (
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]],
        reconnect: Option[FiniteDuration]
)(
        implicit
        c: OkHttpClient,
        d: Decoder[T]
) extends Observable[Event[T]] {
    val channel: Observable[Event[T]] = Observable.unsafeCreate { subscriber ⇒
        import subscriber.scheduler

        val call = WebSocketCall.create( c, request )

        val out = BufferedSubscriber.synchronous( subscriber, strategy )

        call.enqueue( new WebSocketReaderListener[T]( out ) )

        Cancelable { () ⇒
            println( "CANCELLING 1111" )
            call.cancel()
        }
    }

    override def unsafeSubscribeFn( subscriber: Subscriber[Event[T]] ) = {
        channel.unsafeSubscribeFn( subscriber )
    }
}

object WebSocketReader {
    def apply[T](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]] = Default.strategy,
        reconnect: Option[FiniteDuration]                 = Default.reconnect
    )(
        implicit
        c: OkHttpClient,
        d: Decoder[T]
    ): WebSocketReader[T] = {
        new WebSocketReader( request, strategy, reconnect )
    }
}

private class WebSocketReaderListener[T: Decoder](
        downstream: Subscriber.Sync[Event[T]]
) extends OkHttpWebSocketListener {
    var socket: Option[OkHttpWebSocket] = None

    def handle( event: Event[T] ): Unit = {
        if ( downstream.onNext( event ) == Stop ) {
            println( "CLOSING 2222" )
            socket.foreach {
                _.close( Close.GoingAway, "Bye." )
            }
        }
    }

    override def onOpen( socket: OkHttpWebSocket, response: Response ) = {
        this.socket = Some( socket )

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

        handle( Event.Open( socket, message ) )
    }

    override def onMessage( message: ResponseBody ) = {
        val bytes = message.bytes()

        Decoder[T].decode( bytes ) match {
            case Success( message ) ⇒
                logger.debug {
                    s"""
                       |onMessage
                       |  Payload: $message
                    """.stripMargin.trim
                }

                handle( Event.Message( message ) )
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

        handle( Event.Pong( message ) )
    }

    override def onFailure( exception: IOException, response: Response ) = {
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

        handle( Event.Failure( exception, message ) )
        downstream.onError( exception )
    }

    override def onClose( code: Int, reason: String ) = {
        val optionalReason = Option( reason ).filter( _.nonEmpty )

        logger.debug {
            s"""
               |onClose
               |  Code:   $code
               |  Reason: ${optionalReason.orNull}
            """.stripMargin.trim
        }

        handle( Event.Close( code, optionalReason ) )
        downstream.onComplete()
    }
}