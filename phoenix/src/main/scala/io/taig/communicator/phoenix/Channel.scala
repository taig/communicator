package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.OkHttpWebSocket
import io.taig.phoenix.models._
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

case class Channel( topic: Topic )(
        socket:     OkHttpWebSocket,
        val stream: Observable[Inbound],
        timeout:    Duration
) extends io.taig.phoenix.Channel[Observable, Task] {
    override def send( event: Event, payload: Json ): Task[Option[Response]] =
        Phoenix.send( topic, event, payload )( socket, stream, timeout )
}

object Channel {
    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: Duration
    ): Task[Either[Option[Response.Error], Channel]] =
        Phoenix.send( topic, Event.Join )( socket, stream, timeout ).map {
            case Some( Response.Confirmation( _, _, _ ) ) ⇒
                Right( Channel( topic )( socket, stream, timeout ) )
            case Some( error: Response.Error ) ⇒ Left( Some( error ) )
            case None                          ⇒ Left( None )
        }
}