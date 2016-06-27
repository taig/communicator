package io.taig.communicator

import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ FlatSpec, Matchers }

trait Suite
        extends FlatSpec
        with Matchers {
    implicit val client = new OkHttpClient()

    def init[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        Request.Builder().url( server.url( "/" ) )
    }
}