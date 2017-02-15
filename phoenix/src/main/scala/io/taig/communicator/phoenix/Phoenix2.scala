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
import monix.execution.Ack.Stop
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration.{ Inf, Infinite }
import scala.concurrent.duration._

class Phoenix2(
    val socket: OkHttpWebSocket,
    val stream: Observable[Inbound]
)

object Phoenix2 {
    object Event {
        case class Available( phoenix: Phoenix2 ) extends Event
        case object Unavailable extends Event
    }

    def apply(
        request:           OkHttpRequest,
        heartbeat:         Option[FiniteDuration] = Default.heartbeat,
        failureReconnect:  Option[FiniteDuration] = None,
        completeReconnect: Option[FiniteDuration] = None
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create[Event]( OverflowStrategy.Unbounded ) { downstream ⇒
        import downstream.scheduler

        val observable = WebSocket(
            request,
            failureReconnect,
            completeReconnect
        ).publish

        val subscription = observable.connect()

        val stream = observable.doOnNext { event ⇒
            logger.debug( s"Received event: $event" )
        }.collect {
            case WebSocket.Event.Message( Right( message ) ) ⇒
                decode[Inbound]( message ).valueOr( throw _ )
        }

        def next( event: Event ): Unit =
            if ( downstream.onNext( event ) == Stop ) {
                subscription.cancel()
            }

        observable.foreach {
            case WebSocket.Event.Open( socket, _ ) ⇒
                val phoenix = new Phoenix2( socket, stream )
                val available = Event.Available( phoenix )
                next( available )
            case WebSocket.Event.Failure( _, _ ) ⇒ next( Event.Unavailable )
            case WebSocket.Event.Closing( _, _ ) ⇒ next( Event.Unavailable )
            case _                               ⇒ //
        }

        subscription
    }

    sealed trait Event

    def send(
        topic:   Topic,
        event:   PEvent,
        payload: Json   = Json.Null,
        ref:     Ref    = Ref.unique()
    )(
        phoenix: Observable[Phoenix2.Event],
        timeout: FiniteDuration             = Default.timeout
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
            .timeout( timeout )
            .onErrorRecover { case _: TimeoutException ⇒ None }

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
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: FiniteDuration      = Default.timeout
    )(
        implicit
        p: Printer = Printer.noSpaces
    ): Task[Option[Response]] = {
        val request = Request( topic, event, payload, ref )

        val response = stream
            .collect {
                case response: Response ⇒
                    logger.debug( "Received response" )
                    logger.debug( response.toString )
                    response
            }
            .filter( _.ref == request.ref )
            .headOptionL
            .timeout( timeout )
            .onErrorRecover { case _: TimeoutException ⇒ None }

        Task.create { ( scheduler, callback ) ⇒
            val cancelable = response.runAsync( scheduler )

            cancelable.onComplete( callback( _ ) )( scheduler )

            logger.debug( "Sending request" )
            logger.debug( request.asJson.spaces4 )
            socket.send( p.pretty( request.asJson ) )

            cancelable
        }
    }
}