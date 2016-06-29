package io.taig.communicator

import java.util.logging.LogManager

import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ FlatSpec, Matchers }

trait Suite
        extends FlatSpec
        with Matchers {
    LogManager.getLogManager.reset()

    implicit def client = Client()

    def init[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        Request.Builder().url( server.url( "/" ) )
    }
}