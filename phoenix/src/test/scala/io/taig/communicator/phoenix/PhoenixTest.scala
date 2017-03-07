package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

class PhoenixTest extends Suite {
    it should "allow to open a connection" in {
        val websocket = WebSocket( request )
        val phoenix = Phoenix( websocket ).share

        phoenix.collect {
            case Phoenix.Event.Available( phoenix ) ⇒ phoenix
        }.firstL.timeout( 10 seconds ).runAsync.map { phoenix ⇒
            // Close socket explicitly or cancel Observable via .share
            // phoenix.socket.close( 1000, null )
            phoenix.timeout shouldBe 10.seconds
        }
    }

    it should "allow to join a Channel" in {
        val topic = Topic( "echo", "foobar" )

        val websocket = WebSocket( request )
        val phoenix = Phoenix( websocket )
        val channel = Channel.join( phoenix, topic ).share

        channel.collect {
            case Channel.Event.Available( channel ) ⇒ channel
        }.firstL.timeout( 10 seconds ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }

    it should "restore a Phoenix connection when reconnecting after complete" in {
        var counter = 0

        val websocket = WebSocket( request, completeReconnect = _ ⇒ Some( 500 milliseconds ) )
        val phoenix = Phoenix( websocket ).share

        phoenix.collect {
            case Phoenix.Event.Available( phoenix ) ⇒
                if ( counter == 0 ) phoenix.socket.close( 1000, null )
                counter += 1
                counter
        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should contain theSameElementsAs List( 1, 2 )
        }
    }

    //    it should "send a heartbeat" in {
    //        Phoenix(
    //            WebSocket( request ),
    //            heartbeat = Some( 1 second )
    //        ).collect {
    //                case Phoenix.Event.Available( phoenix ) ⇒ phoenix
    //            }.flatMap( _.stream ).collect {
    //                case confirmation: Response.Confirmation ⇒ confirmation
    //            }.firstL.timeout( 10 seconds ).runAsync.map { confirmation ⇒
    //                confirmation.topic shouldBe Topic.Phoenix
    //                confirmation.payload shouldBe Json.obj()
    //            }
    //    }
    //
    //    it should "allow to disable the heartbeat" in {
    //        Phoenix( WebSocket( request ), heartbeat = None ).collect {
    //            case Phoenix.Event.Available( phoenix ) ⇒ phoenix
    //        }.flatMap( _.stream ).collect {
    //            case confirmation: Response.Confirmation ⇒ confirmation
    //        }.firstOptionL
    //            .timeout( 10 seconds )
    //            .onErrorRecover { case _: TimeoutException ⇒ None }
    //            .runAsync
    //            .map( _ shouldBe None )
    //    }

    it should "rejoin a Channel when reconnecting" in {
        var counter = 0

        val topic = Topic( "echo", "foobar" )

        val websocket = WebSocket( request, errorReconnect = _ ⇒ Some( 500 milliseconds ) )
        val phoenix = Phoenix( websocket )
        val channel = Channel.join( phoenix, topic ).share

        channel.collect {
            case Channel.Event.Available( channel ) ⇒
                if ( counter == 0 ) channel.socket.cancel()
                counter += 1
                counter
        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should have length 2
        }
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "foo", "bar" )

        val websocket = WebSocket( request )
        val phoenix = Phoenix( websocket )
        val channel = Channel.join( phoenix, topic ).share

        channel.collect {
            case Channel.Event.Failure( response ) ⇒ response
        }.firstL.timeout( 10 seconds ).runAsync.map {
            _.map( _.message ) shouldBe Some( "unmatched topic" )
        }
    }

    it should "return None when the server omits a response" in {
        val topic = Topic( "echo", "foobar" )

        val websocket = WebSocket( request )
        val phoenix = Phoenix( websocket, timeout = 1 second )
        val channel = Channel.join( phoenix, topic ).share

        val responses = channel.collect {
            case Channel.Event.Message( response: Response ) ⇒ response
        }

        channel.collect {
            case Channel.Event.Available( channel ) ⇒ channel
        }.mapTask { channel ⇒
            channel.send( Event( "no_reply" ), Json.Null )( responses )
        }.firstL.timeout( 10 seconds ).runAsync.map {
            _ should not be defined
        }
    }
}