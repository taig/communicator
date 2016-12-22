package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.phoenix.message.Inbound
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

case class Channel( topic: Topic )(
        socket:     OkHttpWebSocket,
        val stream: Observable[Inbound],
        timeout:    Duration
) {
    def send( event: Event, payload: Json ): Task[Result] =
        Phoenix.send( topic, event, payload )( socket, stream, timeout )

    def leave: Task[Result] = send( Event.Leave, Json.Null )
}

object Channel {
    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: Duration
    ): Task[Either[Error, Channel]] = {
        Phoenix.send( topic, Event.Join )( socket, stream, timeout ).map {
            case Result.Success( _ ) ⇒
                Right( Channel( topic )( socket, stream, timeout ) )
            case error: Error ⇒ Left( error )
        }
    }
}