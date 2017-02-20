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
import monix.execution.{ Cancelable, Scheduler }
import monix.execution.cancelables.{ CompositeCancelable, SerialCancelable }
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
    ): Observable[Event] = Observable.defer {
        val heartbeats = SerialCancelable()

        val composite = CompositeCancelable( heartbeats )

        val cancelable = Cancelable { () ⇒
            logger.debug( "Closing Phoenix connection" )
            composite.cancel()
        }

        def enableHeartbeat(
            socket: OkHttpWebSocket,
            stream: Observable[Inbound]
        )(
            implicit
            s: Scheduler
        ): Unit = heartbeat.foreach { interval ⇒
            logger.debug( s"Enabling heartbeat ($interval)" )
            val cancelable = this.heartbeat( interval ).mapTask { request ⇒
                send( request )( socket, stream, timeout )
            }.subscribe()

            heartbeats := cancelable
            ()
        }

        def cancelHeartbeat(): Unit = {
            if ( heartbeat.isDefined ) {
                logger.debug( "Cancelling heartbeat" )
                heartbeats := Cancelable.empty
                ()
            }
        }

        Observable.create[Event]( strategy ) { downstream ⇒
            import downstream.scheduler

            val observable = websocket.publish

            val connection = observable.connect()

            composite += connection

            val stream = observable.collect {
                case WebSocket.Event.Message( Right( message ) ) ⇒
                    decode[Inbound]( message ).valueOr( throw _ )
            }

            def next( event: Event ): Unit = {
                if ( downstream.onNext( event ) == Stop ) {
                    cancelable.cancel()
                }
            }

            next( Event.Connecting )

            composite += observable.foreach {
                case WebSocket.Event.Open( socket ) ⇒
                    enableHeartbeat( socket, stream )
                    val phoenix = Phoenix( socket, stream, timeout )
                    val available = Event.Available( phoenix )
                    next( available )
                case WebSocket.Event.Failure( _ ) ⇒
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
        }.doOnEarlyStop { () ⇒
            logger.debug( "Early stop shutdown triggered" )
            cancelable.cancel()
        }
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