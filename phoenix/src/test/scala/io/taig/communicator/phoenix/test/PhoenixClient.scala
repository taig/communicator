package io.taig.communicator.phoenix.test

import io.taig.communicator.OkHttpRequest
import io.taig.communicator.phoenix.Phoenix
import io.taig.communicator.test.Suite
import monix.reactive.OverflowStrategy
import org.scalatest.BeforeAndAfterEach

trait PhoenixClient extends BeforeAndAfterEach { this: Suite ⇒
    val request = new OkHttpRequest.Builder()
        .url( s"ws://localhost:4000/socket/websocket" )
        .build()

    var phoenix: Phoenix = null

    override def beforeEach() = {
        super.beforeEach()

        phoenix = Phoenix( request, OverflowStrategy.Unbounded )
    }

    override def afterEach() = {
        super.afterEach()

        phoenix.close()
        phoenix = null
    }
}