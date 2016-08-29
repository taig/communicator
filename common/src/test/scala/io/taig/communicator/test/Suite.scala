package io.taig.communicator.test

import java.util.logging.LogManager

import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ AsyncFlatSpec, Matchers }

trait Suite
        extends AsyncFlatSpec
        with Matchers {
    LogManager.getLogManager.reset()

    implicit val client = new OkHttpClient

    def http[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        new okhttp3.Request.Builder().url( server.url( "/" ) )
    }
}