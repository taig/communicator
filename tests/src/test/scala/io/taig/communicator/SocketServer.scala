package io.taig.communicator

import io.backchat.hookup.HookupClient.Receive
import io.backchat.hookup._
import io.taig.communicator.request.Request.Builder
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future

trait SocketServer extends BeforeAndAfterAll { this: org.scalatest.Suite ⇒
    implicit val client = Client()

    val request = Builder()
        .url( "ws://localhost:9000/ws" )
        .build()

    var send: String ⇒ Future[OperationResult] = null

    def receive: Receive

    val server = HookupServer( 9000 ) {
        new HookupServerClient {
            SocketServer.this.send = send( _: String )
            def receive = SocketServer.this.receive
        }
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