package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Printer
import io.circe.parser.decode
import io.circe.syntax._
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models.{ Inbound, Request, Response, Topic, Event ⇒ PEvent }
import monix.eval.Task
import monix.execution.Cancelable
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
    private val logger = LoggerFactory.getLogger( "phoenix" )

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
    ): Observable[Event] = Observable.create( strategy ) { subscriber ⇒
        val subscription = websocket.collect {
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
        }.doOnNext { event ⇒
            logger.debug( s"Propagating: $event" )
        }.subscribe( subscriber )

        Cancelable { () ⇒
            logger.debug( "Shutdown Phoenix Observable" )
            subscription.cancel()
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

        cancelable.onComplete( r ⇒ logger.debug( "RECEIVED RESULT: " + r ) )( scheduler )
        cancelable.onComplete( callback( _ ) )( scheduler )

        cancelable
    }

    def heartbeat( interval: Option[FiniteDuration] ): Observable[Request] =
        interval.fold( Observable.empty[Request] ) { interval ⇒
            Observable.intervalWithFixedDelay( interval, interval ).map { _ ⇒
                Request( Topic.Phoenix, PEvent( "heartbeat" ) )
            }
        }
}