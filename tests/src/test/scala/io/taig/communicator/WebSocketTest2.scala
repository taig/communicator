package io.taig.communicator

import java.io.IOException

import io.backchat.hookup.{ Connected, Disconnected, TextMessage, _ }
import io.taig.communicator.request.Request
import io.taig.communicator.websocket.{ Close, Event, WebSocket }
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT
import okio.Buffer
import org.scalatest.{ AsyncFlatSpec, BeforeAndAfterAll, Matchers }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketTest2
        extends AsyncFlatSpec
        with Matchers
        with BeforeAndAfterAll {
    implicit val client = Client()

    val request = Request.Builder()
        .url( "ws://localhost:9000/ws" )
        .build()

    val server = HookupServer( 9000 ) {
        new HookupServerClient {
            def receive = {
                case Connected           ⇒ send( "Connected" )
                case TextMessage( text ) ⇒ send( text )
                case Disconnected( _ )   ⇒ //
            }
        }
    }

    override def beforeAll() = {
        super.beforeAll()

        server.start
    }

    override def afterAll() = {
        super.afterAll()

        server.stop
    }

    it should "propagate messages when they are received" in {
        val ( socket, observable ) = WebSocket.apply2[String]( request, OverflowStrategy.Unbounded )

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

        val ( _, observable ) = WebSocket.apply2[String]( request, OverflowStrategy.Unbounded )

        observable.firstL.runAsync.failed.map {
            _ shouldBe a[IOException]
        }
    }
}