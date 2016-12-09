package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import io.taig.communicator.phoenix.message.{ Inbound, Push, Request, Response }
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

class Phoenix(
        socket:     OkHttpWebSocket,
        observable: Observable[WebSocket.Event],
        connection: Cancelable,
        heartbeat:  Cancelable
) {
    val stream: Observable[Inbound] = observable.collect {
        case WebSocket.Event.Message( Right( message ) ) ⇒
            ( decode[Response]( message ): Either[io.circe.Error, Inbound] )
                .orElse( decode[Push]( message ) )
                .valueOr( throw _ )
    }

    def join(
        topic:   Topic,
        payload: Json     = Json.Null,
        timeout: Duration = Default.timeout
    ): Task[Either[Error, Channel]] = {
        Channel.join( topic, payload )(
            socket,
            stream.filter( topic isSubscribedTo _.topic )
        )
    }

    def close(): Unit = {
        heartbeat.cancel()

        val close = socket.close( 1000, null )

        if ( !close ) {
            connection.cancel()
        }
    }
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[WebSocket.Event] = OverflowStrategy.Unbounded,
        heartbeat: Option[FiniteDuration]                        = Some( 7 seconds )
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Task[Phoenix] = Task.defer {
        val observable = WebSocket( request, strategy ).publish
        val connection = observable.connect()

        observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒
                val heartbeats = heartbeat match {
                    case Some( delay ) ⇒
                        this.heartbeat( delay ).foreach( socket.send( _ ) )
                    case None ⇒ Cancelable.empty
                }

                new Phoenix( socket, observable, connection, heartbeats )
        }.firstL
    }

    def heartbeat( delay: FiniteDuration ): Observable[String] = {
        Observable.interval( delay ).map { _ ⇒
            val request = Request( Topic.Phoenix, Event( "heartbeat" ) )
            request.asJson.noSpaces
        }
    }
}