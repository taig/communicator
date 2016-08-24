package io.taig.communicator.test

import io.backchat.hookup._
import io.circe.{ Encoder, Json }
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.taig.communicator.phoenix.message.Response.Status
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.phoenix.{ Event, Topic }
import monix.execution.Scheduler.Implicits.global
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.scalatest.{ AsyncFlatSpec, Matchers }

import scala.language.postfixOps

class PhoenixTest
        extends AsyncFlatSpec
        with Matchers
        with PhoenixClient {
    def generateResponseFor( request: Request ): Json = {
        implicit val encoderStatus: Encoder[Status] = {
            Encoder[String].contramap( _.value )
        }

        Response(
            request.topic,
            Event.Reply,
            Some( Response.Payload( Status.Ok, Json.Null ) ),
            request.ref
        ).asJson
    }

    it should "be possible to join a Channel" in {
        val topic = Topic( "users", "12345" )

        phoenix.join( topic ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }
}