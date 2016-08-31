package io.taig.communicator.websocket.test

import io.backchat.hookup.HookupClient.Receive
import io.backchat.hookup._
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import monix.execution.{ Scheduler, UncaughtExceptionReporter }
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

trait SocketServer extends BeforeAndAfterAll { this: Suite ⇒
    val port = SocketServer.synchronized( SocketServer.port.next() )

    val request = new OkHttpRequest.Builder()
        .url( s"ws://localhost:$port/ws" )
        .build()

    /**
     * Scheduler that does not log exceptions to reduce noise. The library is
     * already explicitly logging socket failures.
     */
    implicit val scheduler = Scheduler.fixedPool(
        "socket-test",
        5,
        reporter = UncaughtExceptionReporter( _ ⇒ {} )
    )

    private val socketServerClient = new HookupServerClient {
        def receive = SocketServer.this.receive
    }

    val server = HookupServer( port )( socketServerClient )

    def receive: Receive

    def send( message: String ): Future[OperationResult] = {
        socketServerClient.send( message )
    }

    def disconnect() = {
        socketServerClient.disconnect()
    }

    def start(): Unit = server.start

    def stop(): Unit = {
        server.stop
        Thread.sleep( 500 )
    }

    override def beforeAll() = {
        super.beforeAll()

        start()
    }

    override def afterAll() = {
        super.afterAll()

        stop()
    }
}

object SocketServer {
    val port = Stream.from( 9000 ).iterator
}