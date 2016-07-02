package io.taig.communicator

import java.util.concurrent.TimeUnit

import okhttp3.mockwebserver.MockResponse

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class CancelTest extends Suite {
    it should "be cancellable" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().throttleBody( 1, 100, TimeUnit.MILLISECONDS ).setBody( "foobar" ) )
        }

        val request = builder.build()
        val future = Request( request ).parse[String].runAsync

        future.cancel()

        Await.result( future.failed, 3 seconds ).getMessage shouldBe "Canceled"
    }
}