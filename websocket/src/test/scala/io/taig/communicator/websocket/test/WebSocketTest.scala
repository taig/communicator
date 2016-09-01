package io.taig.communicator.websocket.test

import java.io.{ EOFException, IOException }
import java.net.UnknownHostException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._

import scala.concurrent.Promise
import scala.language.postfixOps

class WebSocketTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "hello" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    it should "complete when a socket connection is established" in {
        WebSocket[String]( request )( new SimpleWebSocketListener[String]( _ ) )
            .runAsync.map {
                case ( socket, _ ) ⇒
                    socket.close( Close.Normal, Some( "Bye." ) )
                    socket shouldBe an[WebSocket[_]]
            }
    }

    it should "fail if an error occurs while connecting" in {
        val request = new OkHttpRequest.Builder()
            .url( "ws://foobar" )
            .build()

        WebSocket[String]( request )( new SimpleWebSocketListener[String]( _ ) )
            .runAsync
            .failed
            .map {
                _ shouldBe an[UnknownHostException]
            }
    }

    it should "forward messages to the listener" in {
        val promise = Promise[String]()
        val future = promise.future

        val listener: WebSocket[String] ⇒ WebSocketListener[String] = {
            new SimpleWebSocketListener[String]( _ ) {
                override def onMessage( message: String ) = {
                    promise.success( message )
                }
            }
        }

        for {
            ( socket, _ ) ← WebSocket( request )( listener ).runAsync
            assertion ← future.map { _ shouldBe "hello" }
            _ = socket.close( Close.Normal, Some( "Bye." ) )
        } yield assertion
    }

    it should "forward errors to the listener" in {
        val promise = Promise[IOException]()
        val future = promise.future

        val listener: WebSocket[String] ⇒ WebSocketListener[String] = {
            new SimpleWebSocketListener[String]( _ ) {
                override def onFailure( exception: IOException, response: Option[String] ) = {
                    promise.success( exception )
                }
            }
        }

        for {
            ( socket, _ ) ← WebSocket( request )( listener ).runAsync
            _ = stop()
            assertion ← future.map { _ shouldBe an[EOFException] }
            _ = start()
        } yield assertion
    }

    it should "allow to check if the connection has been closed by the user" in {
        WebSocket[String]( request )( new SimpleWebSocketListener[String]( _ ) )
            .runAsync.map {
                case ( socket, _ ) ⇒
                    socket.isClosed shouldBe false
                    socket.close( Close.Normal, Some( "Bye." ) )
                    socket.isClosed shouldBe true
            }
    }

    it should "have no effect when the socket is closed multiple times" in {
        WebSocket[String]( request )( new SimpleWebSocketListener[String]( _ ) )
            .runAsync.map {
                case ( socket, _ ) ⇒
                    socket.close( Close.GoingAway, None )
                    socket.close( Close.GoingAway, None )
                    socket.close( Close.GoingAway, None )
                    socket shouldBe a[WebSocket[_]]
            }
    }

    it should "have no effect when a message is sent to a closed socket" in {
        WebSocket[String]( request )( new SimpleWebSocketListener[String]( _ ) )
            .runAsync.map {
                case ( socket, _ ) ⇒
                    socket.close( Close.GoingAway, None )
                    stop()
                    // Sending a message to the stopped socket would throw an
                    // exception if the library would not prevent it
                    socket.send( "foobar" )
                    start()
                    socket shouldBe a[WebSocket[_]]
            }
    }
}