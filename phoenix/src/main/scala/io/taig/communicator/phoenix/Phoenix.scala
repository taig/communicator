package io.taig.communicator.phoenix

import cats.syntax.either._
import io.circe.Printer
import io.circe.parser.decode
import io.circe.syntax._
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models.{ Inbound, Request, Response, Topic, Event ⇒ PEvent }
import monix.eval.Task
import monix.reactive.Observable
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
        heartbeat: Option[FiniteDuration]      = Default.heartbeat,
        timeout:   FiniteDuration              = Default.timeout
    )(
        implicit
        ohc: OkHttpClient
    ): Observable[Event] = websocket.collect {
        case WebSocket.Event.Connecting   ⇒ Event.Connecting
        case WebSocket.Event.Reconnecting ⇒ Event.Reconnecting
        case WebSocket.Event.Open( socket ) ⇒
            val phoenix = Phoenix( socket, timeout )
            Event.Available( phoenix )
        case WebSocket.Event.Message( Right( message ) ) ⇒
            val value = decode[Inbound]( message ).valueOr( throw _ )
            Event.Message( value )
        case WebSocket.Event.Failure( _ )    ⇒ Event.Unavailable
        case WebSocket.Event.Closing( _, _ ) ⇒ Event.Unavailable
    }.doOnNext { event ⇒
        logger.debug( s"Propagating: $event" )
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
            .headOptionL
            .timeout( timeout )
            .onErrorRecover { case _: TimeoutException ⇒ None }
            .runAsync( scheduler )

        val json = request.asJson
        logger.debug( s"Sending message: ${json.spaces4}" )
        socket.send( p.pretty( json ) )

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