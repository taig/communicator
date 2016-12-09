package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.OkHttpWebSocket
import io.taig.communicator.phoenix.message.{ Inbound, Request, Response }
import io.circe.syntax._
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

class Channel( socket: OkHttpWebSocket )( val topic: Topic ) {
    def send(
        event:   Event,
        payload: Json,
        timeout: Duration = Default.timeout
    ): Task[Result] = ???

    def leave: Task[Result] = send( Event.Leave, Json.Null )
}

object Channel {
    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    )(
        socket: OkHttpWebSocket,
        stream: Observable[Inbound]
    ): Task[Either[Error, Channel]] = {
        val request = Request( topic, Event.Join, payload )

        val channel = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL
            .map {
                case Some( response ) if response.isOk ⇒
                    Result.Success( response )
                case Some( response ) ⇒ Result.Failure( response )
                case None             ⇒ Result.None
            }
            .map {
                case Result.Success( _ ) ⇒
                    Right( new Channel( socket )( topic ) )
                case error: Error ⇒ Left( error )
            }

        val send = Task {
            socket.send( request.asJson.noSpaces )
        }

        Task.mapBoth( channel, send )( ( left, _ ) ⇒ left )
    }
}