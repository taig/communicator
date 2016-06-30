package io.taig.communicator

import java.util.concurrent.TimeUnit

import okhttp3.mockwebserver.MockResponse
import org.scalatest.concurrent.ScalaFutures.whenReady

import scala.language.postfixOps
import scala.concurrent.duration._

class CancelTest extends Suite {
    it should "be cancellable" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().throttleBody( 1, 100, TimeUnit.MILLISECONDS ).setBody( "foobar" ) )
        }

        val request = builder.build()
        val future = Request( request ).parse[String].runAsync

        future.cancel()

        whenReady( future.failed )( _.getMessage shouldEqual "Canceled" )
    }
}