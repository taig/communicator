package io.taig.communicator.test

import io.backchat.hookup.HookupClient.Receive
import io.backchat.hookup._
import io.taig.communicator.Client
import io.taig.communicator.request.Request.Builder
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future

trait SocketServer extends BeforeAndAfterAll { this: org.scalatest.Suite ⇒
    implicit val client = Client()

    val port = SocketServer.synchronized( SocketServer.port.next() )

    val request = Builder()
        .url( s"ws://localhost:$port/ws" )
        .build()

    private val socketServerClient = new HookupServerClient {
        def receive = SocketServer.this.receive
    }

    val server = HookupServer( port )( socketServerClient )

    def receive: Receive

    def send( message: String ): Future[OperationResult] = {
        socketServerClient.send( message )
    }

    def disconnect(): Future[OperationResult] = {
        socketServerClient.disconnect()
    }

    override def beforeAll() = {
        super.beforeAll()

        server.start
    }

    override def afterAll() = {
        super.afterAll()

        server.stop
    }
}

object SocketServer {
    val port = Stream.from( 9000 ).iterator
}