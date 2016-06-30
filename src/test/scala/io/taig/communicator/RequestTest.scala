package io.taig.communicator

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.{ MediaType, RequestBody }
import org.scalatest.concurrent.ScalaFutures.whenReady

import scala.language.{ postfixOps, reflectiveCalls }

class RequestTest extends Suite {
    it should "allow to parse the response" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).parse[String].runAsync )( _.body shouldBe "foobar" )
    }

    it should "allow to ignore the response" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 201 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).ignoreBody.runAsync ) { response ⇒
            intercept[IOException] {
                response.wrapped.body().byteStream().read()
            }
        }
    }

    it should "allow to handle the response manually" in {
        val builder = init { server ⇒
            server.enqueue( new MockResponse().setResponseCode( 201 ).setBody( "foobar" ) )
        }

        val request = builder.build()
        whenReady( Request( request ).unsafeToTask.runAsync ) { response ⇒
            response.wrapped.body().string() shouldBe "foobar"
        }
    }

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

    it should "apply the OkHttp Request.Builder constructor" in {
        Request.Builder() shouldBe an[okhttp3.Request.Builder]
    }
}