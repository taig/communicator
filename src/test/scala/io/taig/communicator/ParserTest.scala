package io.taig.communicator

import java.io.InputStream

import okhttp3.mockwebserver.MockResponse
import org.scalatest.concurrent.ScalaFutures._
import monix.execution.Scheduler.Implicits.global

import scala.io.Source

class ParserTest extends Suite {
    it should "allow to ignore the response" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request.empty( request ).runAsync )( _.code shouldBe 200 )
        whenReady( Request[String]( request ).runAsync )( _.body shouldBe "foobar" )
    }

    it should "support Strings" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request[String]( request ).runAsync )( _.body shouldBe "foobar" )
    }

    it should "support InputStream" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request[InputStream]( request ).runAsync ) { response ⇒
            Source.fromInputStream( response.body ).mkString shouldBe "foobar"
            response.body.close()
        }
    }
}