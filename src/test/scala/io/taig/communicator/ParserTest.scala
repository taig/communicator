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
        whenReady( Request( request ).runAsync )( _.code shouldBe 200 )
        whenReady( Request( request ).parse[String].runAsync )( _.body shouldBe "foobar" )
    }

    it should "support Strings" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).parse[String].runAsync )( _.body shouldBe "foobar" )
    }

    it should "support InputStream" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).parse[InputStream].runAsync ) { response ⇒
            Source.fromInputStream( response.body ).mkString shouldBe "foobar"
            response.body.close()
        }
    }

    it should "be possible to map of a Parser" in {
        val parser = Parser[String].map { ( _, content ) ⇒ content.toUpperCase }

        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).parse( parser ).runAsync )( _.body shouldBe "FOOBAR" )
    }
}