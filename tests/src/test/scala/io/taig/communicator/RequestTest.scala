package io.taig.communicator

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.{ MediaType, RequestBody }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class RequestTest extends Suite {
    it should "apply the OkHttp Request.Builder constructor" in {
        Request.Builder() shouldBe an[okhttp3.Request.Builder]
    }

    it should "allow to parse the response" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).parse[String].runAsync, 3 seconds ).body shouldBe "foobar"
    }

    it should "allow to ignore the response" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 201 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        intercept[IOException] {
            Await.result( Request( request ).ignoreBody.runAsync, 3 seconds )
                .wrapped
                .body()
                .byteStream()
                .read()
        }
    }

    it should "allow to handle the response manually" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 201 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        Await.result( Request( request ).unsafeToTask.runAsync, 3 seconds )
            .wrapped
            .body()
            .string() shouldBe "foobar"
    }

    it should "support GET requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder.build()
        Await.result( Request( request ).runAsync, 3 seconds ).code shouldBe 200
    }

    it should "support POST requests" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 200 ) )
        }

        val request = builder
            .post( RequestBody.create( MediaType.parse( "text/plain" ), "foobar" ) )
            .build()

        Await.result( Request( request ).runAsync, 3 seconds ).code shouldBe 200
    }
}