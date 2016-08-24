package io.taig.communicator.test

import io.taig.communicator.phoenix.Phoenix
import io.taig.communicator.request.Request.Builder
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import okhttp3.OkHttpClient
import org.scalatest.BeforeAndAfterEach

trait PhoenixClient extends BeforeAndAfterEach { this: org.scalatest.Suite ⇒
    implicit val client = new OkHttpClient

    val request = Builder()
        .url( s"ws://localhost:4000/" )
        .build()

    var phoenix: Phoenix = null

    override def beforeEach() = {
        super.beforeEach()

        phoenix = Phoenix( request, OverflowStrategy.Unbounded )
    }

    override def afterEach() = {
        super.afterEach()

        phoenix.close()
    }
}