package io.taig.communicator

import java.io.IOException

import io.backchat.hookup._
import io.taig.communicator.request.Request
import io.taig.communicator.websocket.WebSocket
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import org.scalatest.{ AsyncFlatSpec, Matchers }

import scala.language.postfixOps

class WebSocketTest
        extends AsyncFlatSpec
        with Matchers
        with SocketServer {
    override def receive = {
        case Connected           ⇒ send( "Connected" )
        case TextMessage( text ) ⇒ send( text )
    }

    it should "propagate messages when they are received" in {
        val ( socket, observable ) = WebSocket[String]( request, OverflowStrategy.Unbounded )

        val list = observable.toListL

        socket.onNext( "foobar" )
        socket.onNext( "foo" )
        socket.onNext( "bar" )
        socket.onComplete()

        list.runAsync.map {
            _ should contain theSameElementsAs "Connected" :: "foobar" :: "foo" :: "bar" :: Nil
        }
    }

    it should "not provide a socket and observable instance when failing to connect" in {
        val request = Request.Builder()
            .url( "wss://yourlocalhost" )
            .build()

        val ( _, observable ) = WebSocket[String]( request, OverflowStrategy.Unbounded )

        observable.firstL.runAsync.failed.map {
            _ shouldBe a[IOException]
        }
    }
}