package io.taig.communicator.websocket

import monix.eval.Task

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketTest extends Suite {
    it should "open a connection" in {
        WebSocket( request ).firstL.runAsync.map {
            _ shouldBe WebSocket.Event.Connecting
        }
    }

    it should "establish a connection" in {
        WebSocket( request ).take( 2 ).toListL.runAsync.map {
            case List( connecting, open ) ⇒
                connecting shouldBe WebSocket.Event.Connecting
                open shouldBe a[WebSocket.Event.Open]
        }
    }

    it should "receive echo messages" in {
        val observable = WebSocket( request ).share

        val receive: Task[List[String]] = observable.collect {
            case WebSocket.Event.Message( Right( value ) ) ⇒ value
        }.take( 2 ).toListL

        val send: Task[Unit] = observable.collect {
            case WebSocket.Event.Open( socket ) ⇒ socket
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

    it should "reconnect after failure" in {
        var count = 0

        WebSocket(
            request,
            failureReconnect = Some( 100 milliseconds )
        ).collect {
                case WebSocket.Event.Open( socket ) ⇒
                    socket.cancel()
                    count += 1
                    count
            }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
                _ should contain theSameElementsAs List( 1, 2 )
            }
    }

    it should "reconnect after complete" in {
        var count = 0

        WebSocket(
            request,
            completeReconnect = Some( 100 milliseconds )
        ).collect {
                case WebSocket.Event.Open( socket ) ⇒
                    socket.close( 1000, null )
                    count += 1
                    count
            }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
                _ should contain theSameElementsAs List( 1, 2 )
            }
    }

    it should "not reconnect when cancelled explicitly" in {
        var count = 0

        val observable = WebSocket(
            request,
            failureReconnect  = Some( 100 milliseconds ),
            completeReconnect = Some( 100 milliseconds )
        ).publish

        val subscription = observable.connect()

        observable.collect {
            case WebSocket.Event.Open( socket ) ⇒
                if ( count == 0 ) {
                    socket.close( 1000, null )
                }

                if ( count == 1 ) {
                    subscription.cancel()
                }

                count += 1
                count
        }.take( 3 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should contain theSameElementsAs List( 1, 2 )
        }
    }

    it should "release resources when stopped early" in {
        WebSocket( request ).collect {
            case WebSocket.Event.Open( socket ) ⇒ socket
        }.firstL.runAsync.map {
            _.close( 1000, null ) shouldBe false
        }
    }
}