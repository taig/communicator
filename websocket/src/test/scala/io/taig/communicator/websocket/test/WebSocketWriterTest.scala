package io.taig.communicator.websocket.test

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._

import scala.concurrent.Promise
import scala.language.postfixOps

class WebSocketWriterTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "hello" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    it should "buffer messages until a connection is set up" in {
        val writer = WebSocketWriter[String]
        writer.send( "foo" )
        writer.send( "bar" )
        writer.close( Close.Normal, None )

        val close = Promise[Unit]()

        val messages = collection.mutable.ListBuffer[String]()

        val listener: WebSocket[String] ⇒ WebSocketListener[String] = {
            new SimpleWebSocketListener[String]( _ ) {
                override def onMessage( message: String ) = {
                    messages += message
                }

                override def onClose( code: Int, reason: Option[String] ) = {
                    close.success( {} )
                }
            }
        }

        WebSocket[String]( request )( listener ).runAsync.map {
            case ( socket, _ ) ⇒ writer.connect( socket )
        }

        close.future.map { _ ⇒
            messages should contain theSameElementsAs
                ( "hello" :: "foo" :: "bar" :: Nil )
        }
    }

    it should "buffer messages during an unexpected disconnect" in {
        val writer = WebSocketWriter[String]
        writer.send( "foo" )
        writer.send( "bar" )

        val step = Promise[Unit]()
        val close = Promise[Unit]()

        val messages = collection.mutable.ListBuffer[String]()

        val listener: WebSocket[String] ⇒ WebSocketListener[String] = {
            new SimpleWebSocketListener[String]( _ ) {
                override def onMessage( message: String ) = {
                    messages += message

                    if ( messages.size == 2 ) {
                        step.success( {} )
                    }
                }

                override def onClose( code: Int, reason: Option[String] ) = {
                    close.success( {} )
                }
            }
        }

        val socket = WebSocket[String]( request )( listener ).runAsync.map {
            case ( socket, _ ) ⇒ socket
        }

        socket.foreach( writer.connect )

        step.future.foreach { _ ⇒
            writer.disconnect()
            writer.send( "foobar" )
            writer.close( Close.Normal, None )
            socket.foreach( writer.connect )
        }

        close.future.map { _ ⇒
            messages should contain theSameElementsAs
                ( "hello" :: "foo" :: "bar" :: "foobar" :: Nil )
        }
    }

    it should "allow to check the connection status" in {
        val writer = WebSocketWriter[String]
        writer.send( "foo" )
        writer.send( "bar" )
        writer.close( Close.Normal, None )

        writer.isConnected shouldBe false

        WebSocket[String]( request )( new SimpleWebSocketListener( _ ) )
            .runAsync.map {
                case ( socket, _ ) ⇒
                    writer.connect( socket )
                    writer.isConnected shouldBe true
            }
    }
}