package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

class PhoenixTest extends Suite {
    //    it should "allow to open a connection" in {
    //        val phoenix = Phoenix( WebSocket( request ) )
    //
    //        phoenix.collect {
    //            case Phoenix.Event.Available( phoenix ) ⇒ phoenix
    //        }.share.firstL.timeout( 10 seconds ).runAsync.map { phoenix ⇒
    //            // Close socket explicitly or cancel Observable via .share
    //            // phoenix.socket.close( 1000, null )
    //            phoenix.timeout shouldBe 10.seconds
    //        }
    //    }
    //
    //    it should "allow to join a Channel" in {
    //        val topic = Topic( "echo", "foobar" )
    //
    //        val websocket = WebSocket( request )
    //        val phoenix = Phoenix( websocket )
    //        val channel = Channel.join( phoenix, topic )
    //
    //        channel.collect {
    //            case Channel.Event.Available( channel ) ⇒ channel
    //        }.share.firstL.timeout( 10 seconds ).runAsync.map { channel ⇒
    //            channel.topic shouldBe topic
    //        }
    //    }

    it should "restore a Phoenix connection when reconnecting after close" in {
        val socket = WebSocket.fromRequest(
            request,
            completeReconnect = _ ⇒ Some( 500 milliseconds )
        )
        val phoenix = Phoenix( socket )

        phoenix.collect {
            case Phoenix.Event.Available( phoenix ) ⇒
                phoenix.socket.close( 1000, null )
        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should have length 2
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

    //    it should "rejoin a Channel when reconnecting" in {
    //        val topic = Topic( "echo", "foobar" )
    //
    //        val phoenix = Phoenix(
    //            WebSocket.fromRequest(
    //                request,
    //                errorReconnect = _ ⇒ Some( 500 milliseconds )
    //            )
    //        )
    //        val channel = Channel.join( phoenix, topic )
    //
    //        channel.collect {
    //            case Channel.Event.Available( channel ) ⇒
    //                channel.socket.cancel()
    //        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
    //            _ should have length 2
    //        }
    //    }

    //    it should "fail to join an invalid Channel" in {
    //        val topic = Topic( "foo", "bar" )
    //
    //        val phoenix = Phoenix( WebSocket( request ) )
    //        val channel = Channel.join( topic )( phoenix )
    //
    //        channel.collect {
    //            case Channel.Event.Failure( response ) ⇒ response
    //        }.firstL.timeout( 10 seconds ).runAsync.map {
    //            _.map( _.message ) shouldBe Some( "unmatched topic" )
    //        }
    //    }
    //
    //    it should "return None when the server omits a response" in {
    //        val topic = Topic( "echo", "foobar" )
    //
    //        val phoenix = Phoenix( WebSocket( request ), timeout = 1 second )
    //        val channel = Channel.join( topic )( phoenix )
    //
    //        channel.collect {
    //            case Channel.Event.Available( channel ) ⇒ channel
    //        }.mapTask { channel ⇒
    //            channel.send( Event( "no_reply" ), Json.Null )
    //        }.firstL.timeout( 10 seconds ).runAsync.map {
    //            _ should not be defined
    //        }
    //    }
}