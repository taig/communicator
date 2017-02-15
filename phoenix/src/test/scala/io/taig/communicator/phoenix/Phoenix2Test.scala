package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.phoenix.models._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix2Test extends Suite {
    //    it should "send a heartbeat" in {
    //        for {
    //            phoenix ← Phoenix( request )
    //            inbound ← phoenix.stream.firstL
    //            _ = phoenix.close()
    //        } yield inbound match {
    //            case Response.Confirmation( topic, payload, _ ) ⇒
    //                topic shouldBe Topic.Phoenix
    //                payload shouldBe Json.obj()
    //            case inbound ⇒ fail( s"Received $inbound" )
    //        }
    //    }
    //
    //    it should "allow to disable the heartbeat" in {
    //        for {
    //            phoenix ← Phoenix( request, heartbeat = None )
    //            response ← phoenix.stream
    //                .firstOptionL
    //                .timeout( 10 seconds )
    //                .onErrorRecover { case _: TimeoutException ⇒ None }
    //            _ = phoenix.close()
    //        } yield response shouldBe None
    //    }

    //    it should "allow to close the connection" in {
    //        for {
    //            phoenix ← Phoenix2( request )
    //            _ = phoenix.close()
    //            response ← phoenix.stream.firstOptionL
    //        } yield response shouldBe None
    //    }

    it should "allow to join a Channel" in {
        val topic = Topic( "echo", "foobar" )

        val phoenix = Phoenix2( request ).share
        val channel = Channel2.join( topic )( phoenix )

        channel.collect {
            case Channel2.Event.Available( channel ) ⇒ channel
        }.firstL.timeout( 10 seconds ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }

    it should "rejoin a Channel when reconnecting" in {
        val topic = Topic( "echo", "foobar" )

        val phoenix = Phoenix2(
            request,
            failureReconnect = Some( 500 milliseconds )
        ).share
        val channel = Channel2.join( topic )( phoenix )

        channel.collect {
            case Channel2.Event.Available( channel ) ⇒
                channel.socket.cancel()
                channel
        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should have length 2
        }
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "foo", "bar" )

        val phoenix = Phoenix2( request ).share
        val channel = Channel2.join( topic )( phoenix )

        channel.collect {
            case Channel2.Event.Failure( response ) ⇒ response
        }.firstL.timeout( 10 seconds ).runAsync.map {
            _.map( _.message ) shouldBe Some( "unmatched topic" )
        }
    }
}