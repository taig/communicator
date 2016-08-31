package io.taig.communicator.websocket.test

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketChannelsTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "0" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }
    it should "support writing and reading to/from the socket" in {
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

        Task.zip2( read, write ).runAsync.map {
            case ( result, exception ) ⇒
                result shouldBe "0"
                exception.failed.get shouldBe a[IllegalStateException]
        }
    }
}