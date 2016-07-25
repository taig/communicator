package io.taig.communicator

import java.io.IOException

import monix.reactive.OverflowStrategy
import okhttp3.RequestBody
import okhttp3.ws.{ WebSocket ⇒ OkHttpSocket }
import okio.Buffer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketTest extends Suite {
    val request = Request.Builder()
        .url( "wss://echo.websocket.org" )
        .build()

    it should "complete the Observable when the Socket is closed" in {
        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.close( 1000, "" )
                observable.isEmptyL
        }

        Await.result( task.runAsync, 5 seconds ) shouldBe true
    }

    it should "propagate messages when they are received" in {
        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.sendMessage( RequestBody.create( OkHttpSocket.TEXT, "foobar" ) )
                socket.close( 1000, "" )
                observable.map( new String( _ ) ).firstOptionL
        }

        Await.result( task.runAsync, 5 seconds ) shouldBe Some( "foobar" )
    }

    it should "propagate pings as messages" in {
        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.sendPing( new Buffer().writeUtf8( "foobar" ) )
                socket.close( 1000, "" )
                observable.firstOptionL
        }

        Await.result( task.runAsync, 5 seconds ).map( new String( _ ) ) shouldBe Some( "foobar" )
    }

    it should "not propagate empty pings as messages" in {
        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.sendPing( null )
                socket.close( 1000, "" )
                observable.isEmptyL
        }

        Await.result( task.runAsync, 5 seconds ) shouldBe true
    }

    it should "not provide a socket and observable instance when failing to connect" in {
        val request = Request.Builder()
            .url( "wss://localhost" )
            .build()

        val task = WebSocket( request, OverflowStrategy.Unbounded )

        intercept[IOException] {
            Await.result( task.runAsync, 5 seconds )
        }
    }
}