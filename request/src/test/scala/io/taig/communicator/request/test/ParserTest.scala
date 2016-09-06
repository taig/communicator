package io.taig.communicator.request.test

import io.taig.communicator.request.{ Parser, Request }
import io.taig.communicator.test.Suite
import monix.eval.Task
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse

import scala.language.postfixOps

class ParserTest extends Suite {
    it should "support Byte Array" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()

        Request( request ).parse[Array[Byte]].runAsync.map {
            _.body shouldBe "foobar".getBytes
        }
    }

    it should "support Strings" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()

        Request( request ).parse[String].runAsync.map {
            _.body shouldBe "foobar"
        }
    }

    it should "support ResponseBody" in {
        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()

        Request( request ).parse[ResponseBody].runAsync.map {
            _.body.string shouldBe "foobar"
        }
    }

    it should "be possible to map of a Parser" in {
        val parser = Parser[String].map { ( _, content ) ⇒ content.toUpperCase }

        val builder = http { server ⇒
            server.enqueue( new MockResponse().setBody( "foobar" ) )
        }

        val request = builder.build()

        Request( request ).parse( parser ).runAsync.map {
            _.body shouldBe "FOOBAR"
        }
    }
}