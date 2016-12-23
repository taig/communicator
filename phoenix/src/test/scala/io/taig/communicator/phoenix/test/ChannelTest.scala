package io.taig.communicator.phoenix.test

import cats.data.EitherT
import io.circe.Json
import io.circe.syntax._
import io.taig.communicator.phoenix._
import io.taig.communicator.phoenix.message._
import monix.cats._
import monix.eval.Task

import scala.language.{ implicitConversions, postfixOps }

class ChannelTest extends Suite {
    val topic = Topic( "echo", "foobar" )

    val payload = "foobar".asJson

    it should "allow to leave the Channel" in {
        for {
            phoenix ← EitherT.right( Phoenix( request ) )
            channel ← EitherT( phoenix.join( topic ) )
            response ← EitherT.right( channel.leave )
            _ = phoenix.close()
        } yield response match {
            case Some( Response.Confirmation( topic, _, _ ) ) ⇒
                topic shouldBe this.topic
            case _ ⇒ fail()
        }
    }

    it should "receive echo messages" in {
        for {
            phoenix ← EitherT.right( Phoenix( request ) )
            channel ← EitherT( phoenix.join( topic ) )
            response ← EitherT.right( channel.send( Event( "echo" ), payload ) )
            _ = phoenix.close()
        } yield response match {
            case Some( Response.Confirmation( topic, _, _ ) ) ⇒
                topic shouldBe this.topic
            case _ ⇒ fail( s"Received $response" )
        }
    }

    it should "timeout when the server omits a response" in {
        for {
            phoenix ← EitherT.right( Phoenix( request ) )
            channel ← EitherT( phoenix.join( topic ) )
            result ← EitherT.right( channel.send( Event( "no_reply" ), Json.Null ) )
            _ = phoenix.close()
        } yield {
            result shouldBe None
        }
    }

    it should "handle server pushes" in {
        val payload = Json.obj( "foo" → "bar".asJson )

        for {
            phoenix ← Phoenix( request )
            join ← phoenix.join( topic )
            Right( channel ) = join
            push ← {
                val push = channel.stream
                    .collect { case push: Push ⇒ push }
                    .firstL

                val send = channel.send( Event( "push" ), payload )

                Task.mapBoth( push, send )( ( left, _ ) ⇒ left )
            }
            _ = phoenix.close()
        } yield {
            push.topic shouldBe topic
            push.event shouldBe Event( "answer" )
            push.payload shouldBe payload
        }
    }
}