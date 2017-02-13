package io.taig.communicator.phoenix

import io.taig.communicator.OkHttpRequest
import monix.eval.Task

class WebSocketTest extends Suite {
    override val request = new OkHttpRequest.Builder()
        .url( "wss://echo.websocket.org" )
        .build()

    it should "open a connection" in {
        WebSocket( request ).share.firstL.runAsync.map {
            _ shouldBe a[WebSocket.Event.Open]
        }
    }

    it should "receive echo messages" in {
        val observable = WebSocket( request ).share

        val receive: Task[List[String]] = observable.collect {
            case WebSocket.Event.Message( Right( value ) ) ⇒ value
        }.take( 2 ).toListL

        val send: Task[Unit] = observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒ socket
        }.firstL.foreachL { socket ⇒
            socket.send( "foo" )
            socket.send( "bar" )
            ()
        }

        Task.mapBoth( receive, send )( ( values, _ ) ⇒ values )
            .runAsync
            .map {
                _ should contain theSameElementsAs List( "foo", "bar" )
            }
    }
}