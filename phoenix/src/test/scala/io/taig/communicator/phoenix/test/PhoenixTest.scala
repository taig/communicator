package io.taig.communicator.phoenix.test

import io.circe.Json
import io.taig.communicator.phoenix.{ Event, Phoenix, Ref, Topic }
import io.taig.communicator.phoenix.message.{ Push, Response }
import io.taig.communicator.phoenix.message.Response.Status
import io.taig.communicator.test.Suite
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{ Observable, OverflowStrategy }
import scala.concurrent.duration._

import scala.language.postfixOps

class PhoenixTest
        extends Suite
        with PhoenixClient {
    it should "be possible to join a Channel" in {
        val topic = Topic( "echo", "foobar" )

        phoenix.connect()

        phoenix.join( topic ).runAsync.map {
            _.topic shouldBe topic
        }
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "invalid", "topic" )

        phoenix.connect()

        phoenix.join( topic ).runAsync.failed.map {
            _ shouldBe an[IllegalArgumentException]
        }
    }

    it should "handle failures when trying to join a Channel" in {
        val topic = Topic( "echo", "unauthorized" )

        phoenix.connect()

        phoenix
            .join( topic, Json.obj( "authorized" → Json.fromBoolean( false ) ) )
            .runAsync
            .failed
            .map {
                _ shouldBe an[IllegalArgumentException]
            }
    }

    it should "receive echo messages" in {
        val topic = Topic( "echo", "foo" )
        val payload = Json.obj( "foo" → Json.fromString( "bar" ) )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "echo" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, _, Some( payload ), _ ) ⇒ payload
            }.firstL.runAsync
        } map {
            case Response.Payload( status, message ) ⇒
                status shouldBe Status.Ok
                message shouldBe payload
        }
    }

    it should "support sending JSON values (rather than objects)" in {
        val topic = Topic( "echo", "bar" )
        val payload = Json.fromString( "foo" )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "echo" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, _, Some( payload ), _ ) ⇒ payload
            }.firstL.runAsync
        } map {
            case Response.Payload( status, message ) ⇒
                status shouldBe Status.Ok
                message shouldBe Json.obj( "payload" → payload )
        }
    }

    it should "be possible to disable the heartbeat" in {
        val topic = Topic( "echo", "foobar" )
        val payload = Json.obj( "foo" → Json.fromString( "bar" ) )

        val phoenix = Phoenix(
            request,
            OverflowStrategy.Unbounded,
            heartbeat = None
        )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "echo" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, _, Some( payload ), _ ) ⇒ payload
            }.firstL.runAsync
        }.map {
            case Response.Payload( status, message ) ⇒
                status shouldBe Status.Ok
                message shouldBe payload
        }.andThen {
            case _ ⇒ phoenix.close()
        }
    }

    it should "not get disturbed when the server omits responses" in {
        val topic = Topic( "echo", "foobar" )
        val payload = Json.obj( "foo" → Json.fromString( "bar" ) )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "no_reply" ), Json.Null )
                channel.writer.send( Event( "echo" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, _, Some( payload ), _ ) ⇒ payload
            }.firstL.runAsync
        } map {
            case Response.Payload( status, message ) ⇒
                status shouldBe Status.Ok
                message shouldBe payload
        }
    }

    it should "be able to handle server pushes" in {
        val topic = Topic( "echo", "foobar" )
        val payload = Json.obj( "foo" → Json.fromString( "bar" ) )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "push" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        val response = Response(
            topic,
            Event.Reply,
            Some( Response.Payload( Status.Ok, payload ) ),
            Ref( "1" )
        )

        val push = Push(
            topic,
            Event( "answer" ),
            payload
        )

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.take( 2 ).toListL.runAsync
        } map {
            _ should contain theSameElementsAs ( response :: push :: Nil )
        }
    }

    it should "make server errors accessible" in {
        val topic = Topic( "echo", "foobar" )
        val payload = Json.obj( "answer" → Json.fromInt( 42 ) )

        val channel = phoenix.join( topic ).runAsync

        channel.flatMap { channel ⇒
            Task {
                channel.writer.send( Event( "nonexistant" ), payload )
            }.delayExecution( 500 milliseconds ).runAsync
        }

        phoenix.connect()

        channel.flatMap { channel ⇒
            channel.reader.collect {
                case Response( _, event, _, _ ) ⇒ event
            }.firstL.runAsync
        }.map {
            _ shouldBe Event.Error
        }
    }
}