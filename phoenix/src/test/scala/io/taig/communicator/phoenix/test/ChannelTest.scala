package io.taig.communicator.phoenix.test

import io.circe.Json
import io.taig.communicator.phoenix.message.Response
import io.taig.communicator.phoenix.message.Response.Payload
import io.taig.communicator.phoenix.{ Event, Ref, Topic }
import io.taig.communicator.test.Suite
import monix.eval.Task

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.postfixOps

class ChannelTest
        extends Suite
        with PhoenixClient {
    it should "be possible to leave a Channel" in {
        val topic = Topic( "echo", "foobar" )
        val payload = Json.obj( "foo" → Json.fromString( "bar" ) )

        val channel = phoenix.join( topic ).runAsync
        val hello = Promise[Unit]()

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "echo" ), payload )
            }.delayExecution( 500 millis ).runAsync
        }

        channel.flatMap { channel ⇒
            channel.reader.foreach {
                case Response( _, Event.Reply, Some( Payload( _, `payload` ) ), _ ) ⇒
                    hello.success( {} )
                case _ ⇒ //
            }
        }

        hello.future.flatMap { _ ⇒
            channel.map { channel ⇒
                channel.leave()
            }
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, Event.Close, _, ref ) ⇒ ref
            }.firstL.runAsync
        }.map {
            _ shouldBe Ref( "0" )
        }
    }
}