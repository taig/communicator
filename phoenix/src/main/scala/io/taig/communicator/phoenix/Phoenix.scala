package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Printer
import io.circe.syntax._
import io.circe.parser.decode
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

case class Phoenix(
    socket:  OkHttpWebSocket,
    stream:  Observable[Inbound],
    timeout: FiniteDuration
)

object Phoenix {
    sealed trait Event

    object Event {
        case class Available( phoenix: Phoenix ) extends Event
        case object Unavailable extends Event
    }

    def apply(
        request:           OkHttpRequest,
        heartbeat:         Option[FiniteDuration] = Default.heartbeat,
        timeout:           FiniteDuration         = Default.timeout,
        failureReconnect:  Option[FiniteDuration] = Default.failureReconnect,
        completeReconnect: Option[FiniteDuration] = Default.completeReconnect
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

        val connection = observable.connect()

        val stream = observable.collect {
            case WebSocket.Event.Message( Right( message ) ) ⇒
                decode[Inbound]( message ).valueOr( throw _ )
        }

        var heartbeats = Cancelable.empty

        val cancelable = Cancelable { () ⇒
            heartbeats.cancel()
            connection.cancel()
        }

        def next( event: Event ): Unit = {
            if ( downstream.onNext( event ) == Stop ) {
                cancelable.cancel()
            }
        }

        def enableHeartbeat( socket: OkHttpWebSocket ): Cancelable =
            heartbeat.fold( Cancelable.empty ) { interval ⇒
                this.heartbeat( interval ).mapTask { request ⇒
                    send( request )( socket, stream, timeout )
                }.publish.connect()
            }

        observable.foreach {
            case WebSocket.Event.Open( socket, _ ) ⇒
                heartbeats = enableHeartbeat( socket )
                val phoenix = Phoenix( socket, stream, timeout )
                val available = Event.Available( phoenix )

                next( available )
            case WebSocket.Event.Failure( _, _ ) ⇒
                heartbeats.cancel()
                next( Event.Unavailable )
            case WebSocket.Event.Closing( _, _ ) ⇒
                heartbeats.cancel()
                next( Event.Unavailable )
            case _ ⇒ //
        }

        cancelable
    }

    def send( request: Request )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: FiniteDuration
    )(
        implicit
        p: Printer = Printer.noSpaces
    ): Task[Option[Response]] = {
        val response = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL
            .timeout( timeout )
            .onErrorRecover { case _: TimeoutException ⇒ None }

        Task.create { ( scheduler, callback ) ⇒
            val cancelable = response.runAsync( scheduler )
            cancelable.onComplete( callback( _ ) )( scheduler )
            socket.send( p.pretty( request.asJson ) )
            cancelable
        }
    }

    def heartbeat( interval: FiniteDuration ): Observable[Request] =
        Observable.intervalWithFixedDelay( interval, interval ).map { _ ⇒
            Request( Topic.Phoenix, PEvent( "heartbeat" ) )
        }
}