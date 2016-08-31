package io.taig.communicator.websocket.test

import java.io.IOException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketReaderTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "0" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    it should "receive String messages" in {
        WebSocketReader[String]( request ).collect {
            case Event.Message( value ) ⇒ value
        }.firstL.runAsync.map {
            _ shouldBe "0"
        }
    }

    //    it should "be able to automatically reconnect" in {
    //        val read = WebSocketReader[Int](
    //            request,
    //            reconnect = Some( 500 milliseconds )
    //        ).collect { case Event.Message( value ) ⇒ value }.take( 3 ).toListL
    //
    //        val write = Task {
    //            send( "1" )
    //            // The parser will fail here and throw an exception,
    //            // triggering the reconnect mechanism
    //            send( "not a number" )
    //        }.delayExecution( 500 milliseconds ).flatMap { _ ⇒
    //            Task {
    //                send( "3" )
    //            }.delayExecution( 1000 milliseconds )
    //        }
    //
    //        Task.zip2( read, write ).map( _._1 ).runAsync.map {
    //            _ should contain theSameElementsAs ( 0 :: 1 :: 0 :: 3 :: Nil )
    //        }
    //    }

    //    it should "fail with an unknown url" in {
    //        WebSocketReader[String] {
    //            new OkHttpRequest.Builder()
    //                .url( "ws://externalhost/ws" )
    //                .build()
    //        }.firstL.runAsync.failed.map {
    //            _ shouldBe an[IOException]
    //        }
    //    }
}