package io.taig.communicator.request.test

import java.util.concurrent.TimeUnit

import io.taig.communicator.request.Request
import io.taig.communicator.test.Suite
import okhttp3.mockwebserver.MockResponse

import scala.language.postfixOps

class CancelTest extends Suite {
    it should "be cancellable" in {
        val builder = http { server ⇒
            server.enqueue {
                new MockResponse()
                    .throttleBody( 1, 100, TimeUnit.MILLISECONDS )
                    .setBody( "foobar" )
            }
        }

        val request = builder.build()
        val future = Request( request ).parse[String].runAsync

        future.cancel()

        future.failed.map {
            _.getMessage shouldBe "Canceled"
        }
    }
}