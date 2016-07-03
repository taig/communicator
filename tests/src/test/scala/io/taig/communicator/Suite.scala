package io.taig.communicator

import java.util.logging.LogManager

import monix.execution.Scheduler
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ FlatSpec, Matchers }

trait Suite
        extends FlatSpec
        with Matchers {
    LogManager.getLogManager.reset()

    implicit val scheduler = Scheduler.fixedPool( "tests", 1 )

    implicit def client = Client()

    def init[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        new okhttp3.Request.Builder().url( server.url( "/" ) )
    }
}