package io.taig.communicator

import java.io.IOException

import io.backchat.hookup._
import io.taig.communicator.request.Request
import io.taig.communicator.websocket.{ Close, WebSocket }
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
        val websocket = WebSocket[String]( request, OverflowStrategy.Unbounded )

        val list = websocket.receiver.toListL

        val messages = "foobar" :: "foo" :: "bar" :: Nil

        messages.foreach( websocket.sender.send )
        websocket.sender.close( Close.Normal, "Bye." )

        list.runAsync.map {
            _ should contain theSameElementsAs "Connected" +: messages
        }
    }

    it should "not provide a socket and observable instance when failing to connect" in {
        val request = Request.Builder()
            .url( "wss://yourlocalhost" )
            .build()

        val WebSocket( _, receiver ) = {
            WebSocket[String]( request, OverflowStrategy.Unbounded )
        }

        receiver.firstL.runAsync.failed.map {
            _ shouldBe a[IOException]
        }
    }
}