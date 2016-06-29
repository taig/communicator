package io.taig.communicator

import java.util.concurrent.TimeUnit

import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse
import org.scalatest.concurrent.ScalaFutures.whenReady

import scala.language.postfixOps

class CancelTest extends Suite {
    it should "be cancellable" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ).setBodyDelay( 1, TimeUnit.SECONDS ) )
        }

        val request = builder.build()
        val future = Request( request ).parse[String].runAsync

        future.cancel()

        whenReady( future.failed )( _.getMessage shouldEqual "Canceled" )
    }
}