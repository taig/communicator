package io.taig.communicator.test

import io.taig.communicator.request.{ Parser, Request }
import monix.execution.Scheduler.Implicits.global
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ParserTest extends Suite {
    it should "allow to ignore the response" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
            server.enqueue( new MockResponse().setResponseCode( 200 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).runAsync, 3 seconds ).code shouldBe 200
        Await.result( Request( request ).parse[String].runAsync, 3 seconds ).body shouldBe "foobar"
    }

    it should "support Byte Array" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).parse[Array[Byte]].runAsync, 3 seconds ).body shouldBe "foobar".getBytes
    }

    it should "support Strings" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).parse[String].runAsync, 3 seconds ).body shouldBe "foobar"
    }

    it should "support ResponseBody" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        val response = Await.result( Request( request ).parse[ResponseBody].runAsync, 3 seconds ).body
        response.string shouldBe "foobar"
        response.close()
    }

    it should "be possible to map of a Parser" in {
        val parser = Parser[String].map { ( _, content ) ⇒ content.toUpperCase }

        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).parse( parser ).runAsync, 3 seconds ).body shouldBe "FOOBAR"
    }
}