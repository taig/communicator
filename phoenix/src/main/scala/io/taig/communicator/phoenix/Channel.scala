package io.taig.communicator.phoenix

import io.circe.{ Json, Printer }
import io.taig.communicator.OkHttpWebSocket
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.execution.Cancelable
import monix.reactive.{ Observable, OverflowStrategy }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

case class Channel( topic: Topic )(
        val socket:    OkHttpWebSocket,
        val responses: Observable[Response],
        val timeout:   FiniteDuration
) {
    def send( event: PEvent, payload: Json )(
        implicit
        p: Printer = Default.printer
    ): Task[Option[Response]] = {
        val request = Request( topic, event, payload )
        Phoenix.send( request )( socket, responses, timeout )
    }
}

object Channel {
    private val logger = LoggerFactory.getLogger( "channel" )

    sealed trait Event extends Product with Serializable

    object Event {
        case object Connecting extends Event
        case object Reconnecting extends Event
        case class Available( channel: Channel ) extends Event
        case class Message( value: Inbound ) extends Event

        sealed trait Error extends Event
        case class Failure( response: Option[Response.Error] ) extends Error
        case object Unavailable extends Error
    }

    def join(
        phoenix:  Observable[Phoenix.Event],
        topic:    Topic,
        payload:  Json                                = Json.Null,
        strategy: OverflowStrategy.Synchronous[Event] = OverflowStrategy.Unbounded
    ): Observable[Event] = Observable.create( strategy ) { subscriber ⇒
        import subscriber.scheduler

        val stream = phoenix.publish

        val responses = stream.collect {
            case Phoenix.Event.Message( response: Response ) ⇒ response
        }

        stream.mergeMap {
            case Phoenix.Event.Connecting ⇒ Observable.now( Event.Connecting )
            case Phoenix.Event.Reconnecting ⇒
                Observable.now( Event.Reconnecting )
            case Phoenix.Event.Available( Phoenix( socket, timeout ) ) ⇒
                val request = Request( topic, PEvent.Join, payload )
                val task = Phoenix.send( request )( socket, responses, timeout )
                Observable.fromTask( task ).map {
                    case Some( Response.Confirmation( _, _, _ ) ) ⇒
                        val channel = Channel( topic )( socket, responses, timeout )
                        Event.Available( channel )
                    case Some( error: Response.Error ) ⇒
                        Event.Failure( Some( error ) )
                    case None ⇒ Event.Failure( None )
                }
            case Phoenix.Event.Message( value ) ⇒
                if ( topic isSubscribedTo value.topic )
                    Observable.now( Event.Message( value ) )
                else Observable.empty
            case Phoenix.Event.Unavailable ⇒ Observable.now( Event.Unavailable )
        }.doOnNext { event ⇒
            logger.debug( s"Propagating: $event" )
        }.subscribe( subscriber )

        val subscription = stream.connect()

        Cancelable { () ⇒
            logger.debug( "Shutdown Channel Observable" )
            subscription.cancel()
        }
    }
}