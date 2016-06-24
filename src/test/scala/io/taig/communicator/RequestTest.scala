package io.taig.communicator

import java.util.concurrent.TimeUnit

import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse
import okhttp3.{ MediaType, RequestBody }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.time.{ Milliseconds, Seconds, Span }

import scala.language.reflectiveCalls

class RequestTest extends Suite {
    it should "support GET requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder.build()
        whenReady( Request.empty( request ).runAsync )( _.code shouldBe 200 )
    }

    it should "support POST requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder
            .post( RequestBody.create( MediaType.parse( "text/plain" ), "foobar" ) )
            .build()

        whenReady( Request.empty( request ).runAsync )( _.code shouldBe 200 )
    }

    it should "be cancellable" in {
        implicit val patience = ScalaFutures.PatienceConfig( Span( 10, Seconds ), Span( 500, Milliseconds ) )

        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ).setBodyDelay( 5, TimeUnit.SECONDS ) )
        }

        val request = builder.build()
        val future = Request[String]( request ).runAsync

        global.scheduleOnce( 500, TimeUnit.MILLISECONDS, new Runnable {
            override def run() = future.cancel()
        } )

        whenReady( future.failed )( _.getMessage shouldEqual "Canceled" )
    }
}