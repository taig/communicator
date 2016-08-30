package io.taig.communicator.phoenix.test

import io.circe.Json
import io.taig.communicator.phoenix.Topic
import io.taig.communicator.test.Suite
import monix.execution.Scheduler.Implicits.global

import scala.language.postfixOps

class PhoenixTest
        extends Suite
        with PhoenixClient {
    it should "be possible to join a Channel" in {
        val topic = Topic( "echo", "hello" )

        phoenix.join( topic ).runAsync.map {
            _.topic shouldBe topic
        }
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "invalid", "topic" )

        phoenix.join( topic ).runAsync.failed.map {
            _ shouldBe an[IllegalArgumentException]
        }
    }

    it should "handle failures when trying to join a Channel" in {
        val topic = Topic( "echo", "unauthorized" )

        phoenix
            .join( topic, Json.obj( "authorized" → Json.fromBoolean( false ) ) )
            .runAsync
            .failed
            .map {
                _ shouldBe an[IllegalArgumentException]
            }
    }
}