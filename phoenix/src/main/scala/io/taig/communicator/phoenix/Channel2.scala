package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.OkHttpWebSocket
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.reactive.Observable

case class Channel2( topic: Topic )(
        val socket: OkHttpWebSocket,
        val stream: Observable[Inbound]
) extends io.taig.phoenix.Channel[Observable, Task] {
    override def send( event: PEvent, payload: Json ) = {
        Phoenix2.send2( topic, event, payload )( socket, stream )
    }
}

object Channel2 {
    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    )(
        phoenix: Observable[Phoenix2.Event]
    ): Observable[Event] = phoenix.flatMap {
        case Phoenix2.Event.Available( phoenix ) ⇒
            import phoenix._

            val task = Phoenix2
                .send2( topic, PEvent.Join )( socket, stream )

            Observable.fromTask( task ).map {
                case Some( Response.Confirmation( _, _, _ ) ) ⇒
                    val channel = Channel2( topic )( socket, stream )
                    Event.Available( channel )
                case Some( error: Response.Error ) ⇒
                    Event.Failure( Some( error ) )
                case None ⇒ Event.Failure( None )
            }
        case Phoenix2.Event.Unavailable ⇒
            Observable.now( Event.Unavailable )
    }

    sealed trait Event

    object Event {
        case class Available( channel: Channel2 ) extends Event
        case class Failure( response: Option[Response.Error] ) extends Event
        case object Unavailable extends Event
    }
}