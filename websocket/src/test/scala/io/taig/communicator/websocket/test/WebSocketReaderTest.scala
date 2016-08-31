package io.taig.communicator.websocket.test

import java.io.IOException
import java.net.SocketException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import io.taig.communicator.websocket._
import monix.eval.Task
import monix.execution.{ Scheduler, UncaughtExceptionReporter }
import monix.execution.schedulers.{ AsyncScheduler, ExecutionModel }
import okhttp3.RequestBody
import okhttp3.ws.WebSocket._

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.Inf
import scala.language.postfixOps
import scala.util.Try

class WebSocketReaderTest
        extends Suite
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "hello" )
        case TextMessage( text ) ⇒ send( text )
        case Disconnected( _ )   ⇒ //
    }

    //    ignore should "receive String messages" in {
    //        // Using share because it automatically cancels the connection after
    //        // receiving the message
    //        WebSocketReader[String]( request ).share.collect {
    //            case Event.Message( value ) ⇒ value
    //        }.firstL.runAsync.map {
    //            _ shouldBe "hello"
    //        }
    //    }
    //
    //    it should "close the socket connection when canceled" in {
    //        val promise = Promise[OkHttpWebSocket]()
    //        val future = promise.future
    //
    //        val cancelable = WebSocketReader[String]( request ).foreach {
    //            case Event.Open( socket, _ ) ⇒
    //                println( "LE HAPPEN =)" )
    //                promise.success( socket )
    //            case event ⇒ //
    //        }
    //
    //        future.map { socket ⇒
    //            println( "About to cancel" )
    //            cancelable.cancel()
    //
    //            Thread.sleep( 5000 )
    //
    //            val send = Try {
    //                socket.sendMessage(
    //                    RequestBody.create( TEXT, "foobar" )
    //                )
    //            }
    //
    //            send.isFailure shouldBe true
    //            send.failed.get shouldBe a[IllegalStateException]
    //            send.failed.get.getMessage shouldBe "closed"
    //        }
    //    }

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