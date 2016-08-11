package io.taig.communicator.phoenix

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.taig.communicator._
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.websocket.{ OkHttpWebSocket, WebSocket }
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{ Observable, OverflowStrategy }

import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix(
        socket:     OkHttpWebSocket,
        observable: Observable[Response],
        heartbeat:  Option[Duration],
        reconnect:  Option[Duration]
) {
    private val iterator: Iterator[Ref] = {
        Stream.iterate( 0L )( _ + 1 ).map( Ref( _ ) ).iterator
    }

    private def ref = synchronized( iterator.next() )

    def join( topic: Topic, payload: Json = Json.Null ): Task[Option[Channel]] = {
        val id = ref

        // TODO it's necessary to subscribe at first, and then send the message, right?!

        val send = Task {
            val message = Request( topic, Event.Join, payload, id )
            socket.sendMessage( request( message.asJson ) )
        }

        val receive = observable.findL( _.ref == id ).map {
            _.filter( _.payload.status == "ok" ).map { response ⇒
                new Channel( this, response.topic )
            }
        }

        for {
            _ ← send
            receive ← receive
        } yield receive
    }

    def close(): Unit = socket.close( 1001, "Disconnected by client" )
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Array[Byte]],
        heartbeat: Option[Duration]                          = Some( 7 seconds ),
        reconnect: Option[Duration]                          = Some( 5 seconds )
    )(
        implicit
        c: Client,
        s: Scheduler
    ): Task[Phoenix] = WebSocket( request, strategy ).map {
        case ( socket, observable ) ⇒
            val json = observable.map { data ⇒
                decode[Response]( new String( data, "UTF-8" ) ).valueOr( throw _ )
            }

            new Phoenix( socket, json, heartbeat, reconnect )
    }
}