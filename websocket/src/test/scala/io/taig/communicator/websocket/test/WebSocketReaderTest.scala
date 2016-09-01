package io.taig.communicator.websocket.test

import java.net.UnknownHostException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._
import monix.eval.Task

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketReaderTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "hello" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    it should "receive String messages" in {
        // Using share because it automatically cancels the connection after
        // receiving the message
        WebSocketReader[String]( request ).share.collect {
            case Event.Message( value ) ⇒ value
        }.firstL.runAsync.map {
            _ shouldBe "hello"
        }
    }

    it should "close the socket connection when canceled" in {
        val socket = Promise[WebSocket[String]]()
        val close = Promise[Unit]()

        val cancelable = WebSocketReader[String]( request ).foreach {
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

    it should "automatically reconnect if an error occurs" in {
        var connected = false

        val reader = WebSocketReader[String](
            request,
            reconnect = Some( 500 milliseconds )
        ).share

        val socket = reader.collect {
            case Event.Open( socket, _ ) ⇒
                if ( !connected ) {
                    restart()
                }

                connected = true
                socket
        }.take( 2 ).toListL

        val messages = reader.collect {
            case Event.Message( value ) ⇒ value
        }.take( 2 ).toListL

        Task.zip2( socket, messages ).runAsync.map {
            case ( sockets, messages ) ⇒
                sockets.last.close( Close.Normal, Some( "Bye." ) )
                messages should contain theSameElementsAs
                    ( "hello" :: "hello" :: Nil )
        }
    }

    it should "automatically reconnect if the server closes the connection" in {
        var connected = false

        val reader = WebSocketReader[String](
            request,
            reconnect = Some( 500 milliseconds )
        ).share

        val socket = reader.collect {
            case Event.Open( socket, _ ) ⇒
                if ( !connected ) {
                    disconnect()
                }

                connected = true
                socket
        }.take( 2 ).toListL

        val messages = reader.collect {
            case Event.Message( value ) ⇒ value
        }.take( 2 ).toListL

        Task.zip2( socket, messages ).runAsync.map {
            case ( sockets, messages ) ⇒
                sockets.last.close( Close.Normal, Some( "Bye." ) )
                messages should contain theSameElementsAs
                    ( "hello" :: "hello" :: Nil )
        }
    }

    it should "fail if an error occurs while connecting" in {
        val request = new OkHttpRequest.Builder()
            .url( "ws://foobar" )
            .build()

        WebSocketReader[String]( request, reconnect = None )
            .firstL
            .runAsync
            .failed
            .map { _ shouldBe an[UnknownHostException] }
    }
}