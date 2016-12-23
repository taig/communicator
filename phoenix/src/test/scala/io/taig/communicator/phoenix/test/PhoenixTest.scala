package io.taig.communicator.phoenix.test

import io.taig.communicator.phoenix.{ Phoenix, Topic }
import io.taig.communicator.phoenix.message._
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
        } yield response match {
            case Response.Confirmation( topic, _, _ ) ⇒
                topic shouldBe Topic.Phoenix
            case _ ⇒ fail( s"Received $response" )
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
        } yield response shouldBe None
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
        } yield channel match {
            case Left( Some( Response.Error( topic, message, _ ) ) ) ⇒
                topic shouldBe topic
                message shouldBe "unmatched topic"
            case response ⇒ fail( s"Received $response" )
        }
    }
}