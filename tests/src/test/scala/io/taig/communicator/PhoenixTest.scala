package io.taig.communicator

import io.backchat.hookup._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.phoenix.{ Event, Phoenix, Topic }
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.scalatest.{ AsyncFlatSpec, BeforeAndAfterAll, BeforeAndAfterEach, Matchers }

import scala.language.postfixOps

class PhoenixTest
        extends AsyncFlatSpec
        with Matchers
        with SocketServer
        with PhoenixClient {
    override def receive = {
        case JsonMessage( json ) ⇒
            val serialized = Serialization.write( json )( DefaultFormats )
            val request = decode[Request]( serialized ).valueOr( throw _ )
            val response = generateResponseFor( request )
            send( response.asJson.spaces4 )
    }

    def generateResponseFor( request: Request ): Response = {
        Response(
            request.topic,
            Event.Reply,
            Response.Payload( "ok", Json.Null ),
            request.ref
        )
    }

    it should "be possible to join a Channel" in {
        val topic = Topic( "users", "12345" )

        phoenix.join( topic ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }
}