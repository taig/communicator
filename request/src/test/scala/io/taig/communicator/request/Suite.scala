package io.taig.communicator.request

import monix.eval.Task
import monix.execution.{ Scheduler, UncaughtExceptionReporter }
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.mockwebserver.MockWebServer
import org.scalatest.{ Assertion, AsyncFlatSpec, Matchers }

import scala.concurrent.Future
import scala.language.implicitConversions

trait Suite
        extends AsyncFlatSpec
        with Matchers {
    implicit val client = new OkHttpClient

    /**
     * Scheduler that does not log exceptions to reduce noise. The library is
     * already explicitly logging socket failures.
     */
    implicit val scheduler = Scheduler.fixedPool(
        "test",
        5,
        reporter = UncaughtExceptionReporter( _ ⇒ {} )
    )

    def http[U]( f: MockWebServer ⇒ U ): Builder = {
        val server = new MockWebServer
        f( server )
        server.start()
        new okhttp3.Request.Builder().url( server.url( "/" ) )
    }

    implicit def taskToFuture(
        task: Task[Assertion]
    ): Future[Assertion] = task.runAsync
}