package io.taig.communicator

import java.net.{ Socket, URI }

import io.backchat.hookup._
import io.taig.communicator.request.Request
import io.taig.communicator.websocket.WebSocket
import io.taig.communicator.websocket.OkHttpWebSocket
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockWebServer
import okhttp3.ws.WebSocket.TEXT
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class PhoenixTest
        extends Suite
        with BeforeAndAfterAll {
    val request = Request.Builder()
        .url( "ws://localhost:9000/ws" )
        .build()

    val server = HookupServer( 9000 ) {
        new HookupServerClient {
            def receive = {
                case Connected           ⇒ send( "Connected" )
                case TextMessage( text ) ⇒ send( text )
                case Disconnected( _ )   ⇒ send( "Disconnected" )
            }
        }
    }

    override def beforeAll() = {
        super.beforeAll()

        server.start
    }

    override def afterAll() = {
        super.afterAll()

        println( "Stopping..." )

        server.stop
    }

    it should "complete the Observable when the Socket is closed" in {
        val task = WebSocket( request, OverflowStrategy.Unbounded ).flatMap {
            case ( socket, observable ) ⇒
                val send = Task {
                    socket.sendMessage( RequestBody.create( TEXT, "yolo" ) )
                }

                val receive = observable.headL.map( new String( _ ) )

                for {
                    _ ← send
                    receive ← receive
                } yield receive
        }

        Await.result( task.runAsync, 5 seconds ) == "yolo"
    }
}