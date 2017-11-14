package io.taig.communicator.request

import java.util.concurrent.TimeUnit

import okhttp3.mockwebserver.MockResponse

class CancelTest extends Suite {
  it should "be cancellable" in {
    val builder = http { server ⇒
      server.enqueue {
        new MockResponse()
          .throttleBody(1, 100, TimeUnit.MILLISECONDS)
          .setBody("foobar")
      }
    }

    val request = builder.build()
    val future = Request(request).parse[String].runAsync

    future.cancel()

    future.failed.map {
      _.getMessage shouldBe "Canceled"
    }
  }
}
