package io.taig.communicator.websocket.test

import io.backchat.hookup.HookupClient.Receive
import io.backchat.hookup._
import io.taig.communicator.OkHttpRequest
import io.taig.communicator.test.Suite
import monix.execution.{ Scheduler, UncaughtExceptionReporter }
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._
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

    private val server = HookupServer( port )( socketServerClient )

    private var stopped = Promise[Unit]()

    server.onStop {
        stopped.success( {} )
        stopped = Promise[Unit]()
    }

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
        Await.result( stopped.future, 3 seconds )
    }

    def restart(): Unit = {
        stop()
        start()
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