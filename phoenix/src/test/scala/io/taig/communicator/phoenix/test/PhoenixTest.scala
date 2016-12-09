package io.taig.communicator.phoenix.test

import io.taig.communicator.phoenix.{ Event, Phoenix, Result, Topic }
import cats.syntax.either._

import scala.concurrent.TimeoutException
import scala.language.postfixOps
import scala.concurrent.duration._

class PhoenixTest extends Suite {
    it should "send a heartbeat" in {
        for {
            phoenix ← Phoenix( request )
            response ← phoenix.stream.firstL
            _ = phoenix.close()
        } yield {
            response.topic shouldBe Topic.Phoenix
            response.event shouldBe Event.Reply
        }
    }

    it should "allow to disable the heartbeat" in {
        for {
            phoenix ← Phoenix( request, heartbeat = None )
            response ← phoenix.stream
                .firstOptionL
                .timeout( 10 seconds )
                .onErrorRecover { case _: TimeoutException ⇒ None }
            _ = phoenix.close()
        } yield {
            response shouldBe None
        }
    }

    it should "allow to close the connection" in {
        for {
            phoenix ← Phoenix( request )
            _ = phoenix.close()
            response ← phoenix.stream.firstOptionL
        } yield response shouldBe None
    }

    it should "allow to join a Channel" in {
        val topic = Topic( "echo", "foobar" )

        for {
            phoenix ← Phoenix( request )
            channel ← phoenix.join( topic )
            _ = phoenix.close()
        } yield channel.map( _.topic ) shouldBe Right( topic )
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "foo", "bar" )

        for {
            phoenix ← Phoenix( request )
            channel ← phoenix.join( topic )
            _ = phoenix.close()
        } yield {
            channel match {
                case Left( Result.Failure( response ) ) ⇒
                    response.isError shouldBe true
                    response.event shouldBe Event.Reply
                    response.topic shouldBe topic
                    response.error shouldBe Some( "unmatched topic" )
                case _ ⇒ fail()
            }
        }
    }
}