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
        WebSocket( request )( NoopWebSocketListener[String] ).runAsync.map {
            case ( socket, _ ) ⇒
                socket.close( Close.Normal, "Bye." )
                socket shouldBe an[OkHttpWebSocket]
        }
    }

    it should "fail if an error occurs while connecting" in {
        val request = new OkHttpRequest.Builder()
            .url( "ws://foobar" )
            .build()

        WebSocket( request )( NoopWebSocketListener[String] ).runAsync.failed.map {
            _ shouldBe an[UnknownHostException]
        }
    }

    it should "forward messages to the listener" in {
        val promise = Promise[String]()
        val future = promise.future

        val listener: OkHttpWebSocket ⇒ NoopWebSocketListener[String] = {
            new NoopWebSocketListener[String]( _ ) {
                override def onMessage( message: String ) = {
                    promise.success( message )
                }
            }
        }

        for {
            ( socket, _ ) ← WebSocket( request )( listener ).runAsync
            assertion ← future.map { _ shouldBe "hello" }
            _ = socket.close( Close.Normal, "Bye." )
        } yield assertion
    }

    it should "forward errors to the listener" in {
        val promise = Promise[IOException]()
        val future = promise.future

        val listener: OkHttpWebSocket ⇒ NoopWebSocketListener[String] = {
            new NoopWebSocketListener[String]( _ ) {
                override def onFailure( exception: IOException, response: Option[String] ) = {
                    promise.success( exception )
                }
            }
        }

        for {
            ( socket, _ ) ← WebSocket( request )( listener ).runAsync
            _ = server.stop
            assertion ← future.map { _ shouldBe an[EOFException] }
            _ = server.start
        } yield assertion
    }
}