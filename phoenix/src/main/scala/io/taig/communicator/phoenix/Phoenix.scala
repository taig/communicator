package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Printer
import io.circe.parser.decode
import io.circe.syntax._
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.phoenix.Phoenix.Event
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models.{ Inbound, Request, Response, Topic, Event ⇒ PEvent }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.execution.cancelables.SerialCancelable
import monix.reactive.observers.Subscriber
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

case class Phoenix(
    socket:  OkHttpWebSocket,
    timeout: FiniteDuration
)

object Phoenix {
    private[phoenix] val logger = LoggerFactory.getLogger( "phoenix" )

    sealed trait Action extends Product with Serializable

    object Action {
        case class Forward( event: Event ) extends Action
        case class Heartbeat( request: Request, phoenix: Phoenix ) extends Action
    }

    sealed trait Event extends Product with Serializable

    object Event {
        case object Connecting extends Event
        case object Reconnecting extends Event
        case class Available( phoenix: Phoenix ) extends Event
        case class Message( value: Inbound ) extends Event
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

        def startHeartbeat(
            phoenix:   Phoenix,
            responses: Observable[Response]
        )(
            implicit
            s: Scheduler
        ): Unit = heartbeat.foreach { interval ⇒
            logger.debug( s"Starting heartbeat ($interval)" )
            heartbeats := Phoenix.heartbeat( interval ).mapTask { request ⇒
                Phoenix.send( request )( phoenix.socket, responses, phoenix.timeout )
            }.subscribe()
        }

        def stopHeartbeat(): Unit =
            if ( heartbeat.isDefined && !heartbeats.isCanceled ) {
                logger.debug( s"Stopping heartbeat" )
                heartbeats := Cancelable.empty
            }

        def cancelHeartbeat(): Unit =
            if ( heartbeat.isDefined && !heartbeats.isCanceled ) {
                logger.debug( s"Cancelling heartbeat" )
                heartbeats.cancel()
            }

        Observable.create( strategy ) { subscriber ⇒
            import subscriber.scheduler

            val observable = websocket.collect {
                case WebSocket.Event.Connecting   ⇒ Event.Connecting
                case WebSocket.Event.Reconnecting ⇒ Event.Reconnecting
                case WebSocket.Event.Open( socket ) ⇒
                    val phoenix = Phoenix( socket, timeout )
                    Event.Available( phoenix )
                case WebSocket.Event.Message( Right( message ) ) ⇒
                    val value = decode[Inbound]( message ).valueOr( throw _ )
                    Event.Message( value )
                case _: WebSocket.Event.Failure | _: WebSocket.Event.Closing ⇒
                    Event.Unavailable
            }.publish

            val responses = observable.collect {
                case Event.Message( response: Response ) ⇒ response
            }

            val subscription = observable.connect()

            observable.doOnNext { event ⇒
                logger.debug( s"Propagating: $event" )

                event match {
                    case Event.Available( phoenix ) ⇒
                        startHeartbeat( phoenix, responses )
                    case Event.Unavailable ⇒ stopHeartbeat()
                    case _                 ⇒ //
                }
            }.subscribe( subscriber )

            Cancelable { () ⇒
                logger.debug( "Shutdown Phoenix Observable" )
                cancelHeartbeat()
                subscription.cancel()
            }
        }.doOnTerminate { _ ⇒
            cancelHeartbeat()
        }
    }

    def send( request: Request )(
        socket:    OkHttpWebSocket,
        responses: Observable[Response],
        timeout:   FiniteDuration
    )(
        implicit
        p: Printer = Printer.noSpaces
    ): Task[Option[Response]] = Task.create { ( scheduler, callback ) ⇒
        val cancelable = responses
            .filter( _.ref == request.ref )
            .doOnSubscribe { () ⇒
                val json = request.asJson
                logger.debug( s"Sending message: ${json.spaces4}" )
                socket.send( p.pretty( json ) )
                ()
            }
            .headOptionL
            .timeout( timeout )
            .onErrorRecover { case _: TimeoutException ⇒ None }
            .runAsync( scheduler )

        cancelable.onComplete( callback( _ ) )( scheduler )

        cancelable
    }

    def heartbeat( interval: FiniteDuration ): Observable[Request] =
        Observable.intervalWithFixedDelay( interval, interval ).map { _ ⇒
            Request( Topic.Phoenix, PEvent( "heartbeat" ) )
        }
}