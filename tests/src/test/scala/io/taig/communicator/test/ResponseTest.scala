package io.taig.communicator.test

import io.taig.communicator.request.{ Request, Response }
import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.MockResponse

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ResponseTest extends Suite {
    it should "have a useful toString implementation" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        val response = Await.result( Request( request ).parse[String].runAsync, 3 seconds )
        response.toString should fullyMatch regex """
             |>>> http://localhost:\d+/
             |\[No headers\]
             |<<< 200 OK
             |Content-Length: 6
            """.stripMargin.trim
    }

    it should "proxy all okhttp.Response methods" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        import collection.JavaConversions._

        val request = builder.build()
        val response = Await.result( Request( request ).parse[String].runAsync, 3 seconds )
        response.code shouldBe response.wrapped.code()
        response.message.orNull shouldBe response.wrapped.message()
        response.headers shouldBe response.wrapped.headers()
        response.request shouldBe response.wrapped.request()
        response.protocol shouldBe response.wrapped.protocol()
        response.handshake.orNull shouldBe response.wrapped.handshake()
        response.isSuccessful shouldBe response.wrapped.isSuccessful
        response.isRedirect shouldBe response.wrapped.isRedirect
        seqAsJavaList( response.challenges ) shouldBe response.wrapped.challenges()
        response.cacheControl shouldBe response.wrapped.cacheControl()
        response.sentRequestAtMillis shouldBe response.wrapped.sentRequestAtMillis()
        response.receivedResponseAtMillis shouldBe response.wrapped.receivedResponseAtMillis()
        response.cacheResponse.map( _.wrapped ).orNull shouldBe response.wrapped.cacheResponse()
        response.networkResponse.map( _.wrapped ).orNull shouldBe response.wrapped.networkResponse()
        response.priorResponse.map( _.wrapped ).orNull shouldBe response.wrapped.priorResponse()
    }

    it should "unapply a Response" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 404 ) )
        }

        val request = builder.build()
        Await.result( Request( request ).runAsync, 3 seconds ).code shouldBe 404
    }

    it should "unapply a Response.With" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 201 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        val Response.With( code, body ) = Await.result( Request( request ).parse[String].runAsync, 3 seconds )
        code shouldBe 201
        body shouldBe "foobar"
    }
}