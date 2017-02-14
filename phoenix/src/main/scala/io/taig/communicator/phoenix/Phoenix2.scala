package io.taig.communicator.phoenix

import java.util.concurrent.TimeUnit.MILLISECONDS

import cats.syntax.either._
import io.circe.{ Json, Printer }
import io.circe.syntax._
import io.circe.parser.decode
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import io.taig.phoenix.models.Response.Error
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration.{ Inf, Infinite }
import scala.concurrent.duration._

class Phoenix2(
        val socket: OkHttpWebSocket,
        val stream: Observable[Inbound]
) extends io.taig.phoenix.Phoenix[Observable, Task] {
    override def join( topic: Topic, payload: Json ) = ???

    override def close(): Unit = ???
}

object Phoenix2 {
    object Event {
        case class Available( phoenix: Phoenix2 ) extends Event
        case object Unavailable extends Event
    }

    def apply(
        request:           OkHttpRequest,
        strategy:          OverflowStrategy.Synchronous[WebSocket.Event] = OverflowStrategy.Unbounded,
        heartbeat:         Option[FiniteDuration]                        = Default.heartbeat,
        failureReconnect:  Option[FiniteDuration]                        = None,
        completeReconnect: Option[FiniteDuration]                        = None
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Observable[Event] = Observable.defer {
        val observable = WebSocket(
            request,
            strategy,
            failureReconnect,
            completeReconnect
        ).publish

        val subscription = observable.connect()

        val stream = observable.collect {
            case WebSocket.Event.Message( Right( message ) ) ⇒
                decode[Inbound]( message ).valueOr( throw _ )
        }

        observable.flatMap {
            case WebSocket.Event.Open( socket, _ ) ⇒
                Observable.now( Event.Available( new Phoenix2( socket, stream ) ) )
            case WebSocket.Event.Failure( _, _ ) ⇒
                Observable.now( Event.Unavailable )
            case WebSocket.Event.Closing( _, _ ) ⇒
                Observable.now( Event.Unavailable )
            case _ ⇒ Observable.empty
        }.doOnSubscriptionCancel { () ⇒
            subscription.cancel()
        }.replay( 1 )
    }

    sealed trait Event

    def send(
        topic:   Topic,
        event:   PEvent,
        payload: Json   = Json.Null,
        ref:     Ref    = Ref.unique()
    )(
        phoenix: Observable[Phoenix2.Event]
    )(
        implicit
        p: Printer = Printer.noSpaces
    ): Task[Option[Response]] = Task.defer {
        val request = Request( topic, event, payload, ref )

        val available = phoenix
            .collect { case Phoenix2.Event.Available( phoenix ) ⇒ phoenix }

        val response = available
            .flatMap( _.stream )
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL

        Task.create { ( scheduler, callback ) ⇒
            response
                .runAsync( scheduler )
                .onComplete( callback( _ ) )( scheduler )

            available.map( _.socket ).firstL.foreach { socket ⇒
                socket.send( p.pretty( request.asJson ) )
                ()
            }( scheduler )
        }
    }

    def send2(
        topic:   Topic,
        event:   PEvent,
        payload: Json   = Json.Null,
        ref:     Ref    = Ref.unique()
    )(
        socket: OkHttpWebSocket,
        stream: Observable[Inbound]
    )(
        implicit
        p: Printer = Printer.noSpaces
    ): Task[Option[Response]] = {
        val request = Request( topic, event, payload, ref )

        val response = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL

        Task.create { ( scheduler, callback ) ⇒
            val cancelable = response.runAsync( scheduler )

            cancelable.onComplete( callback( _ ) )( scheduler )

            socket.send( p.pretty( request.asJson ) )

            cancelable
        }
    }
}