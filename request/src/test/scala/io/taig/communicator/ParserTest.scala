package io.taig.communicator

import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse

class ParserTest extends Suite {
  it should "support Byte Array" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[Array[Byte]].runToFuture.map {
      _.body shouldBe "foobar".getBytes
    }
  }

  it should "support Strings" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[String].runToFuture.map {
      _.body shouldBe "foobar"
    }
  }

  it should "support ResponseBody" in {
    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse[ResponseBody].runToFuture.map {
      _.body.string shouldBe "foobar"
    }
  }

  it should "be possible to map of a Parser" in {
    val parser = Parser[String].map(_.toUpperCase)

    val builder = http { server ⇒
      server.enqueue(new MockResponse().setBody("foobar"))
    }

    val request = builder.build()

    Request(request).parse(parser).runToFuture.map {
      _.body shouldBe "FOOBAR"
    }
  }
}
