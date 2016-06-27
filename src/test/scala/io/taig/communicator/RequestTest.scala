package io.taig.communicator

import java.util.concurrent.TimeUnit

import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse
import okhttp3.{ MediaType, RequestBody }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.time.{ Milliseconds, Seconds, Span }

import scala.concurrent.duration._
import scala.language.{ postfixOps, reflectiveCalls }
import scala.util.{ Failure, Success }

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

    it should "be cancellable" in {
        implicit val patience = ScalaFutures.PatienceConfig( Span( 10, Seconds ), Span( 500, Milliseconds ) )

        val request = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ).setBodyDelay( 1, TimeUnit.SECONDS ) )
        } build ()

        val future = Request( request ).parse[String].runAsync

        future.cancel()

        global.scheduleOnce( 250 milliseconds ) {
            future.cancel()
        }

        whenReady( future.failed )( _.getMessage shouldEqual "Canceled" )
    }
}