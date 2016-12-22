package io.taig.communicator.phoenix

import java.util.concurrent.TimeUnit

import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{ Json, Error ⇒ CirceError }
import io.taig.communicator.phoenix.message.{ Inbound, Push, Request, Response }
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.{ Inf, Infinite }
import scala.language.postfixOps

class Phoenix(
        socket:     OkHttpWebSocket,
        observable: Observable[WebSocket.Event],
        connection: Cancelable,
        heartbeat:  Cancelable,
        timeout:    Duration
) {
    val stream: Observable[Inbound] = observable.collect {
        case WebSocket.Event.Message( Right( message ) ) ⇒
            ( decode[Response]( message ): Either[CirceError, Inbound] )
                .orElse( decode[Push]( message ) )
                .valueOr( throw _ )
    }

    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    ): Task[Either[Error, Channel]] = {
        Channel.join( topic, payload )(
            socket,
            stream.filter( topic isSubscribedTo _.topic ),
            timeout
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

        val timeout = ohc.readTimeoutMillis() match {
            case 0            ⇒ Inf
            case milliseconds ⇒ Duration( milliseconds, TimeUnit.MILLISECONDS )
        }

        observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒
                val heartbeats = heartbeat match {
                    case Some( delay ) ⇒
                        this.heartbeat( delay ).foreach( socket.send( _ ) )
                    case None ⇒ Cancelable.empty
                }

                new Phoenix(
                    socket,
                    observable,
                    connection,
                    heartbeats,
                    timeout
                )
        }.firstL
    }

    def send(
        topic:   Topic,
        event:   Event,
        payload: Json  = Json.Null,
        ref:     Ref   = Ref.unique()
    )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: Duration
    ): Task[Result] = {
        val request = Request( topic, event, payload, ref )

        val channel = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL
            .map {
                case Some( confirmation: Response.Confirmation ) ⇒
                    Result.Success( confirmation )
                case Some( error: Response.Error ) ⇒ Result.Failure( error )
                case None                          ⇒ Result.None
            }

        val withTimeout = timeout match {
            case _: Infinite ⇒ channel
            case timeout: FiniteDuration ⇒
                channel.timeout( timeout ).onErrorRecover {
                    case _: TimeoutException ⇒ Result.None
                }
        }

        val send = Task {
            socket.send( request.asJson.noSpaces )
        }

        Task.mapBoth( withTimeout, send )( ( left, _ ) ⇒ left )
    }

    def heartbeat( delay: FiniteDuration ): Observable[String] = {
        Observable.interval( delay ).map { _ ⇒
            val request = Request( Topic.Phoenix, Event( "heartbeat" ) )
            request.asJson.noSpaces
        }
    }
}