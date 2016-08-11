package io.taig.communicator.phoenix

import cats.data.Xor
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.taig.communicator._
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.websocket.WebSocket
import monix.eval.Task
import monix.reactive.{ Observable, Observer, OverflowStrategy }

import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix(
        observer:   Observer[Json],
        observable: Observable[Json],
        heartbeat:  Option[Duration],
        reconnect:  Option[Duration]
) {
    private val iterator: Iterator[Ref] = {
        Stream.iterate( 0L )( _ + 1 ).map( Ref( _ ) ).iterator
    }

    private def withRef[T]( f: Ref ⇒ T ): T = f( synchronized( iterator.next() ) )

    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel] = withRef { ref ⇒
        val send = Task {
            val message = Request( topic, Event.Join, payload, ref )
            observer.onNext( message.asJson )
        }

        val receive = observable.map( _.as[Response] ).collect {
            case Xor.Right( Response( topic, _, payload, `ref` ) ) if payload.status == "ok" ⇒
                new Channel( this, topic )
        }.firstL

        for {
            _ ← send
            receive ← receive
        } yield receive
    }

    def close(): Unit = observer.onComplete()
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Json],
        heartbeat: Option[Duration]                   = Some( 7 seconds ),
        reconnect: Option[Duration]                   = Some( 5 seconds )
    )(
        implicit
        c: Client
    ): Phoenix = {
        val ( observer, observable ) = WebSocket[Json]( request, strategy )
        new Phoenix( observer, observable, heartbeat, reconnect )
    }
}