package io.taig.communicator

import io.backchat.hookup._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.phoenix.{ Event, Phoenix, Topic }
import io.taig.communicator.request.Request.Builder
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.scalatest.{ AsyncFlatSpec, BeforeAndAfterAll, Matchers }

import scala.language.postfixOps

class PhoenixTest
        extends AsyncFlatSpec
        with Matchers
        with BeforeAndAfterAll {
    implicit val client = Client()

    val request = Builder()
        .url( "ws://localhost:9000/ws" )
        .build()

    val server = HookupServer( 9000 ) {
        new HookupServerClient {
            def receive = {
                case JsonMessage( json ) ⇒
                    val serialized = Serialization.write( json )( DefaultFormats )
                    val request = decode[Request]( serialized ).valueOr( throw _ )
                    val response = generateResponseFor( request )
                    send( response.asJson.spaces4 )
            }
        }
    }

    def generateResponseFor( request: Request ): Response = {
        Response(
            request.topic,
            Event.Reply,
            Response.Payload( "ok", Json.Null ),
            request.ref
        )
    }

    override def beforeAll() = {
        super.beforeAll()

        server.start
    }

    override def afterAll() = {
        super.afterAll()

        server.stop
    }

    it should "be possible to join a Channel" in {
        val phoenix = Phoenix( request, OverflowStrategy.Unbounded )

        val topic = Topic( "users", "12345" )

        phoenix.join( topic ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }.andThen {
            case _ ⇒ phoenix.close()
        }
    }
}