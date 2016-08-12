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
        val buffer = new BufferedOkHttpWebSocket

        var socket: OkHttpWebSocket = null

        val observable = Observable.create( strategy ) { downstream ⇒
            def handle( data: Array[Byte] ): Unit = {
                co.decode( data ) match {
                    case Success( data ) ⇒
                        if ( downstream.onNext( data ) == Stop ) {
                            buffer.close( Close.GoingAway, "Bye." )
                        }
                    case Failure( exception ) ⇒
                        downstream.onError( exception )

                }
            }

            logger.info( s"Connecting to websocket ${request.url()}" )

            val call = WebSocketCall.create( cl, request )

            call.enqueue {
                new WebSocketListener {
                    override def onOpen( websocket: OkHttpWebSocket, response: Response ) = {
                        logger.debug {
                            val message = Option( response )
                                .flatMap( response ⇒ Option( response.body() ) )
                                .map( _.string() )
                                .orNull

                            s"""
                              |[${request.url()}] onOpen:
                              |$message
                            """.stripMargin.trim
                        }

                        socket = websocket
                        buffer.inject( websocket )
                    }

                    override def onMessage( response: ResponseBody ) = {
                        val message = response.bytes()

                        logger.debug {
                            s"""
                               |[${request.url()}] onMessage:
                               |${new String( message )}
                             """.stripMargin.trim
                        }

                        handle( message )
                    }

                    override def onPong( payload: Buffer ) = {
                        val message = Option( payload ).map( _.readByteArray() )

                        logger.debug {
                            s"""
                              |[${request.url()}] onPing:
                              |${message.map( new String( _ ) ).orNull}
                            """.stripMargin.trim
                        }

                        handle( message.getOrElse( Array.emptyByteArray ) )
                    }

                    override def onClose( code: Int, reason: String ) = {
                        logger.debug(
                            s"""
                               |[${request.url()}] onClose:
                               |$code $reason
                             """.stripMargin.trim
                        )

                        downstream.onComplete()
                    }

                    override def onFailure( exception: IOException, response: Response ) = {
                        logger.debug( {
                            val message = Option( response )
                                .flatMap( response ⇒ Option( response.body() ) )
                                .map( _.string() )
                                .orNull

                            s"""
                               |[${request.url()}] onFailure:
                               |$message
                             """.stripMargin.trim
                        }, exception )

                        downstream.onError( exception )
                    }
                }
            }

            Cancelable( () ⇒ buffer.close( Close.GoingAway, "Bye." ) )
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

