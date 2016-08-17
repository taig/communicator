package io.taig.communicator.test

import java.util.logging.LogManager

import io.taig.communicator.Client
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ FlatSpec, Matchers }

trait Suite
        extends FlatSpec
        with Matchers {
    LogManager.getLogManager.reset()

    implicit val client = Client()

    def http[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        new okhttp3.Request.Builder().url( server.url( "/" ) )
    }
}