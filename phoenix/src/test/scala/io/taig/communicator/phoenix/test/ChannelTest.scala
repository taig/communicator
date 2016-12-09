package io.taig.communicator.phoenix.test

import cats.data.EitherT
import io.circe.syntax._
import io.taig.communicator.phoenix._
import monix.cats._

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
        } yield {
            response match {
                case Result.Success( response ) ⇒
                    response.isOk shouldBe true
                    response.event shouldBe Event.Reply
                    response.topic shouldBe topic
                    response.error shouldBe None
                case _ ⇒ fail()
            }
        }
    }

    //    it should "receive echo messages" in {
    //        for {
    //            phoenix ← Phoenix( request )
    //            Right( channel ) ← phoenix.join( topic )
    //            Result.Success( response ) ← channel.send( Event( "echo" ), payload )
    //            _ = phoenix.close()
    //        } yield {
    //            response.event shouldBe Event.Reply
    //            response.topic shouldBe topic
    //        }
    //    }
    //
    //    it should "timeout when the server omits a response" in {
    //        for {
    //            phoenix ← Phoenix( request )
    //            Right( channel ) ← phoenix.join( topic )
    //            result ← channel.send( Event( "no_reply" ), Json.Null )
    //            _ = phoenix.close()
    //        } yield {
    //            result shouldBe Result.None
    //        }
    //    }
    //
    //    it should "handle server pushes" in {
    //        val payload = Json.obj( "foo" → "bar".asJson )
    //
    //        for {
    //            phoenix ← Phoenix( request )
    //            Right( channel ) ← phoenix.join( topic )
    //            push ← {
    //                val push = Source.empty[( Event, Json, Ref )]
    //                    .via( channel.flow )
    //                    .collect { case push: Push ⇒ push }
    //                    .toMat( Sink.head[Push] )( Keep.right )
    //                    .run()
    //                val send = channel.send( Event( "push" ), payload )
    //
    //                send.flatMap( _ ⇒ push )
    //            }
    //            _ = phoenix.close()
    //        } yield {
    //            push.topic shouldBe topic
    //            push.event shouldBe Event( "answer" )
    //            push.payload shouldBe payload
    //        }
    //    }
}