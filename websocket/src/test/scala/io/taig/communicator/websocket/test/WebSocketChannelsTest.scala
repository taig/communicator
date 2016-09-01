package io.taig.communicator.websocket.test

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._

import scala.concurrent.Promise
import scala.language.postfixOps

class WebSocketChannelsTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "hello" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    it should "support writing and reading to/from the socket" in {
        val channels = WebSocketChannels[String]( request )
        val WebSocketChannels( reader, writer ) = channels

        writer.send( "foo" )
        writer.send( "bar" )
        writer.send( "foobar" )

        reader.collect {
            case Event.Message( value ) ⇒ value
        }.take( 4 ).toListL.runAsync.map { messages ⇒
            channels.close()
            messages should contain theSameElementsAs
                ( "hello" :: "foo" :: "bar" :: "foobar" :: Nil )
        }
    }

    it should "close the socket connection when the reader is canceled" in {
        val socket = Promise[WebSocket[String]]()
        val close = Promise[Unit]()

        val WebSocketChannels( reader, _ ) = {
            WebSocketChannels[String]( request )
        }

        val cancelable = reader.foreach {
            case Event.Open( s, _ )  ⇒ socket.success( s )
            case Event.Close( _, _ ) ⇒ close.success( {} )
            case event               ⇒ //
        }

        socket.future.map { socket ⇒
            cancelable.cancel()
        }

        socket.future.flatMap { socket ⇒
            close.future.map { _ ⇒
                socket.isClosed shouldBe true
            }
        }
    }
}