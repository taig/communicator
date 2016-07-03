package io.taig.communicator

import monix.reactive.OverflowStrategy
import okhttp3.RequestBody
import okhttp3.ws.{ WebSocket ⇒ OkHttpSocket }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketTest extends Suite {
    it should "complete the Observable when the Socket is closed" in {
        val request = Request.Builder()
            .url( "wss://echo.websocket.org" )
            .build()

        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.close( 1000, "" )
                observable.isEmptyL
        }

        Await.result( task.runAsync, 5 seconds ) shouldBe true
    }

    it should "propagate messages when they are received" in {
        val request = Request.Builder()
            .url( "wss://echo.websocket.org" )
            .build()

        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                socket.sendMessage( RequestBody.create( OkHttpSocket.TEXT, "foobar" ) )
                socket.close( 1000, "" )
                observable.map( new String( _ ) ).firstOptionL
        }

        Await.result( task.runAsync, 5 seconds ) shouldBe Some( "foobar" )
    }
}