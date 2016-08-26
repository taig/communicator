package io.taig.communicator.test

import java.io.IOException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.request.Request.Builder
import io.taig.communicator.websocket._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{ AsyncFlatSpec, Matchers }

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.Try

class WebSocketTest
        extends AsyncFlatSpec
        with Matchers
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "0" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    "WebSocketWriter" should "connect to a websocket" in {
        WebSocketWriter[String]( request ).map { writer ⇒
            writer.close( Close.GoingAway, None )
            writer shouldBe a[WebSocketWriter[_]]
        }.runAsync
    }

    it should "allow to send a payload" in {
        WebSocketWriter[String]( request ).map { writer ⇒
            writer.send( "foobar" )
            writer.close( Close.GoingAway, None )
            writer shouldBe a[WebSocketWriter[_]]
        }.runAsync
    }

    it should "allow to send a ping" in {
        WebSocketWriter[String]( request ).map { writer ⇒
            writer.ping()
            writer.ping( Some( "foobar" ) )
            writer.close( Close.GoingAway, None )
            writer shouldBe a[WebSocketWriter[_]]
        }.runAsync
    }

    it should "have no effect when the socket is closed multiple times" in {
        WebSocketWriter[String]( request ).map { writer ⇒
            writer.close( Close.GoingAway, None )
            writer.close( Close.GoingAway, None )
            writer.close( Close.GoingAway, None )
            writer shouldBe a[WebSocketWriter[_]]
        }.runAsync
    }

    it should "fail to write to a closed websocket" in {
        WebSocketWriter[String]( request ).map { writer ⇒
            writer.send( "foo" )
            writer.close( Close.GoingAway, Some( "Bye." ) )
            Try( writer.send( "bar" ) ).failed.get shouldBe a[IllegalStateException]
        }.runAsync
    }

    it should "fail with an unknown url" in {
        WebSocketWriter[String] {
            Builder()
                .url( "ws://externalhost/ws" )
                .build()
        }.runAsync.failed.map {
            _ shouldBe an[IOException]
        }
    }

    it should "work synchronously with a buffered connection" in {
        val writer = BufferedWebSocketWriter[String]()
        writer.send( "foobar" )
        writer.close( Close.Normal, None )
        writer shouldBe a[BufferedWebSocketWriter[_]]
    }

    it should "buffer messages during an unexpected disconnect" in {
        Task.create[List[String]] { ( scheduler, callback ) ⇒
            val writer = BufferedWebSocketWriter[String]()

            val messages = collection.mutable.ListBuffer[String]()

            val listener = new WebSocketListener[String] {
                override def onMessage( message: String ) = {
                    messages += message
                }

                override def onPong( payload: Option[String] ) = {}

                override def onClose( code: Int, reason: Option[String] ) = {
                    callback.onSuccess( messages.toList )
                }

                override def onFailure( exception: IOException, response: Option[String] ) = {}
            }

            writer.send( "foo" )

            WebSocket[String]( request )( listener ).runAsync( scheduler ).map {
                case ( socket, _ ) ⇒
                    writer.connect( socket )
                    writer.disconnect()
                    writer.send( "bar" )
                    writer.connect( socket )
                    writer.close( Close.Normal, Some( "Bye." ) )
            }

        }.runAsync.map {
            _ should contain theSameElementsAs ( "0" :: "foo" :: "bar" :: Nil )
        }
    }

    "WebSocketReader" should "receive String messages" in {
        WebSocketReader[String]( request ).collect {
            case Event.Message( value ) ⇒ value
        }.firstL.runAsync.map {
            _ shouldBe "0"
        }
    }

    it should "be able to automatically reconnect" in {
        val read = WebSocketReader[Int](
            request,
            reconnect = Some( 500 milliseconds )
        ).collect { case Event.Message( value ) ⇒ value }.take( 4 ).toListL

        val write = Task {
            send( "1" )
            // The parser will fail here and throw an exception,
            // triggering the reconnect mechanism
            send( "not a number" )
        }.delayExecution( 500 milliseconds ).flatMap { _ ⇒
            Task {
                send( "3" )
            }.delayExecution( 1000 milliseconds )
        }

        Task.mapBoth( read, write ) {
            case ( result, _ ) ⇒ result
        }.runAsync.map {
            _ should contain theSameElementsAs
                ( 0 :: 1 :: 0 :: 3 :: Nil )
        }
    }

    it should "fail with an unknown url" in {
        WebSocketReader[String] {
            Builder()
                .url( "ws://externalhost/ws" )
                .build()
        }.firstL.runAsync.failed.map {
            _ shouldBe an[IOException]
        }
    }

    "WebSocketChannels" should "support writing and reading to/from the socket" in {
        val WebSocketChannels( reader, writer ) = {
            WebSocketChannels.symmetric[String]( request )
        }

        writer.send( "foo" )
        writer.send( "bar" )
        writer.send( "foobar" )
        writer.close( Close.Normal, None )

        reader.collect {
            case Event.Message( value ) ⇒ value
        }.take( 4 ).toListL.runAsync.map {
            _ should contain theSameElementsAs
                ( "0" :: "foo" :: "bar" :: "foobar" :: Nil )
        }
    }

    it should "be able to automatically close the socket" in {
        val WebSocketChannels( reader, writer ) = {
            WebSocketChannels.symmetric[String]( request )
        }

        // Share observable to auto-cancel after execution
        val read = reader.share.collect {
            case Event.Message( value ) ⇒ value
        }.firstL

        // Try to write something on the socket afterwards
        val write = Task {
            writer.send( "foobar" )
        }.delayExecution( 500 milliseconds ).materialize

        Task.mapBoth( read, write ) { case result ⇒ result }.runAsync.map {
            case ( result, exception ) ⇒
                result shouldBe "0"
                exception.failed.get shouldBe a[IllegalStateException]
        }
    }
}