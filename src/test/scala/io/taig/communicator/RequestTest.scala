package io.taig.communicator

import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse
import okhttp3.{ MediaType, RequestBody }
import org.scalatest.concurrent.ScalaFutures.whenReady

import scala.language.{ postfixOps, reflectiveCalls }

class RequestTest extends Suite {
    it should "support GET requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder.build()
        whenReady( Request( request ).runAsync )( _.code shouldBe 200 )
    }

    it should "support POST requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder
            .post( RequestBody.create( MediaType.parse( "text/plain" ), "foobar" ) )
            .build()

        whenReady( Request( request ).runAsync )( _.code shouldBe 200 )
    }
}