package io.taig.communicator.phoenix

import cats.data.EitherT
import io.circe.Json
import io.circe.syntax._
import io.taig.phoenix.models._
import monix.cats._
import monix.eval.Task

class ChannelTest extends Suite {
    val topic = Topic( "echo", "foobar" )

    val payload = "foobar".asJson

    it should "receive echo messages" in {
        for {
            phoenix ← EitherT.right[Task, Option[Response.Error], Phoenix]( Phoenix( request ) )
            channel ← EitherT[Task, Option[Response.Error], Channel]( phoenix.join( topic ) )
            response ← EitherT.right[Task, Option[Response.Error], Option[Response]]( channel.send( Event( "echo" ), payload ) )
            _ = phoenix.close()
        } yield response match {
            case Some( Response.Confirmation( topic, _, _ ) ) ⇒
                topic shouldBe this.topic
            case _ ⇒ fail( s"Received $response" )
        }
    }

    it should "timeout when the server omits a response" in {
        for {
            phoenix ← EitherT.right[Task, Option[Response.Error], Phoenix]( Phoenix( request ) )
            channel ← EitherT[Task, Option[Response.Error], Channel]( phoenix.join( topic ) )
            result ← EitherT.right[Task, Option[Response.Error], Option[Response]]( channel.send( Event( "no_reply" ), Json.Null ) )
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
            push ← join match {
                case Right( channel ) ⇒
                    val push = channel.stream
                        .collect { case push: Push ⇒ push }
                        .firstL

                    val send = channel.send( Event( "push" ), payload )

                    Task.mapBoth( push, send )( ( left, _ ) ⇒ left )
                case Left( _ ) ⇒ Task.raiseError( new IllegalStateException() )
            }
            _ = phoenix.close()
        } yield {
            push.topic shouldBe topic
            push.event shouldBe Event( "answer" )
            push.payload shouldBe payload
        }
    }
}