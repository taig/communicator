package io.taig.communicator

import java.util.logging.LogManager

import monix.execution.Scheduler
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ FlatSpec, Matchers }

trait Suite
        extends FlatSpec
        with Matchers {
    LogManager.getLogManager.reset()

    implicit val scheduler = Scheduler.singleThread( "tests" )

    implicit val patience = ScalaFutures.PatienceConfig(
        timeout  = Span( 5, Seconds ),
        interval = Span( 500, Millis )
    )

    implicit def client = Client()

    def init[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        new okhttp3.Request.Builder().url( server.url( "/" ) )
    }
}