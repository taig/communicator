package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Printer
import io.circe.parser.decode
import io.circe.syntax._
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.execution.cancelables.{ CompositeCancelable, MultiAssignmentCancelable }
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
        case object Connecting extends Event
        case object Reconnecting extends Event
        case class Available( phoenix: Phoenix ) extends Event
        case object Unavailable extends Event
    }

    def apply(
        websocket: Observable[WebSocket.Event],
        strategy:  OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded,
        heartbeat: Option[FiniteDuration]              = Default.heartbeat,
        timeout:   FiniteDuration                      = Default.timeout
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = Observable.create[Event]( strategy ) { downstream ⇒
        import downstream.scheduler

        val heartbeats = MultiAssignmentCancelable()

        val observable = websocket.publish

        val connection = observable.connect()

        val composite = CompositeCancelable( heartbeats, connection )

        val stream = observable.collect {
            case WebSocket.Event.Message( Right( message ) ) ⇒
                decode[Inbound]( message ).valueOr( throw _ )
        }

        val cancelable = Cancelable { () ⇒
            logger.debug( "Closing Phoenix connection" )
            composite.cancel()
        }

        def next( event: Event ): Unit = {
            if ( downstream.onNext( event ) == Stop ) {
                cancelable.cancel()
            }
        }

        def enableHeartbeat( socket: OkHttpWebSocket ): Cancelable =
            heartbeat.map { interval ⇒
                logger.debug( s"Enabling heartbeat ($interval)" )
                this.heartbeat( interval ).mapTask { request ⇒
                    send( request )( socket, stream, timeout )
                }.publish.connect()
            }.getOrElse( Cancelable.empty )

        def cancelHeartbeat(): Unit = {
            if ( heartbeat.isDefined ) {
                logger.debug( "Cancelling heartbeat" )
            }

            heartbeats.cancel()
        }

        next( Event.Connecting )

        composite += observable.foreach {
            case WebSocket.Event.Open( socket, _ ) ⇒
                heartbeats := enableHeartbeat( socket )

                val phoenix = Phoenix( socket, stream, timeout )
                val available = Event.Available( phoenix )
                next( available )
            case WebSocket.Event.Failure( _, _ ) ⇒
                cancelHeartbeat()
                next( Event.Unavailable )
            case WebSocket.Event.Closing( _, _ ) ⇒
                cancelHeartbeat()
                next( Event.Unavailable )
            case WebSocket.Event.Reconnecting ⇒
                next( Event.Reconnecting )
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

            val json = request.asJson

            logger.debug( s"Sending message: ${json.spaces4}" )

            socket.send( p.pretty( json ) )

            cancelable
        }
    }

    def heartbeat( interval: FiniteDuration ): Observable[Request] =
        Observable.intervalWithFixedDelay( interval, interval ).map { _ ⇒
            Request( Topic.Phoenix, PEvent( "heartbeat" ) )
        }
}