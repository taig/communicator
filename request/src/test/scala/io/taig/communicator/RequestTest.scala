package io.taig.communicator

import okhttp3.mockwebserver.MockResponse
import okhttp3.{MediaType, RequestBody}

class RequestTest extends Suite {
  it should "allow to parse the response" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runAsync.map {
      _.body shouldBe "foobar"
    }
  }

  it should "allow to ignore the response" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setResponseCode(201).setBody("foobar"))
    }

    val request = builder.build()

    Request(request).runAsync.map {
      _.code shouldBe 201
    }
  }

  it should "allow to handle the response manually" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setResponseCode(201).setBody("foobar"))
    }

    val request = builder.build()

    Request(request).unsafeToTask.runAsync.map {
      _.wrapped.body.string shouldBe "foobar"
    }
  }

  it should "support GET requests" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setResponseCode(200))
    }

    val request = builder.build()

    Request(request).runAsync.map {
      _.code shouldBe 200
    }
  }

  it should "support POST requests" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setResponseCode(200))
    }

    val request = builder
      .post(RequestBody.create(MediaType.parse("text/plain"), "foobar"))
      .build()

    Request(request).runAsync.map {
      _.code shouldBe 200
    }
  }
}
