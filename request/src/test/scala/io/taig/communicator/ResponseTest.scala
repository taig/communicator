package io.taig.communicator

import okhttp3.mockwebserver.MockResponse

class ResponseTest extends Suite {
  it should "have a useful toString implementation" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runToFuture.map {
      _.toString should fullyMatch regex """
                 |>>> http://localhost:\d+/
                 |\[No headers\]
                 |<<< 200 OK
                 |Content-Length: 6
                """.stripMargin.trim
    }
  }

  it should "proxy all okhttp.Response methods" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    import collection.JavaConverters._

    val request = builder.build()

    Request(request).parse[String].runToFuture.map { response ⇒
      response.code shouldBe response.wrapped.code()
      response.message.orNull shouldBe response.wrapped.message()
      response.headers shouldBe response.wrapped.headers()
      response.request shouldBe response.wrapped.request()
      response.protocol shouldBe response.wrapped.protocol()
      response.handshake.orNull shouldBe response.wrapped.handshake()
      response.isSuccessful shouldBe response.wrapped.isSuccessful
      response.isRedirect shouldBe response.wrapped.isRedirect
      response.challenges shouldBe iterableAsScalaIterableConverter(
        response.wrapped.challenges()).asScala.toList
      response.cacheControl shouldBe response.wrapped.cacheControl()
      response.sentRequestAtMillis shouldBe response.wrapped
        .sentRequestAtMillis()
      response.receivedResponseAtMillis shouldBe response.wrapped
        .receivedResponseAtMillis()
      response.cacheResponse.map(_.wrapped).orNull shouldBe response.wrapped
        .cacheResponse()
      response.networkResponse.map(_.wrapped).orNull shouldBe response.wrapped
        .networkResponse()
      response.priorResponse.map(_.wrapped).orNull shouldBe response.wrapped
        .priorResponse()
    }
  }

  it should "unapply a Response" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setResponseCode(201).setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runToFuture.map {
      case Response(code, body) ⇒
        code shouldBe 201
        body shouldBe "foobar"
    }
  }
}
