package io.taig.communicator.test

import java.util.concurrent.TimeUnit

import io.taig.communicator.request.Request
import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class CancelTest extends Suite {
    ignore should "be cancellable" in {
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

        Await.result( future.failed, 3 seconds ).getMessage shouldBe "Canceled"
    }
}