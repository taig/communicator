package io.taig.communicator.test

import io.taig.communicator.phoenix.Topic
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{ AsyncFlatSpec, Matchers }

import scala.language.postfixOps

class PhoenixTest
        extends AsyncFlatSpec
        with Matchers
        with PhoenixClient {
    it should "be possible to join a Channel" in {
        val topic = Topic( "echo", "hello" )

        phoenix.join( topic ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }
    
    it should "fail to join an invalid Channel" in {
        val topic = Topic( "invalid", "topic" )

        phoenix.join( topic ).runAsync.failed.map {
            _ shouldBe an [IllegalArgumentException]
        }
    }
}