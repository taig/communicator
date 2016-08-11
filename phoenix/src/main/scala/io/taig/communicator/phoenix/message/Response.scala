package io.taig.communicator.phoenix.message

import io.circe.Json
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

case class Response(
    topic:   Topic,
    event:   Event,
    payload: Response.Payload,
    ref:     Ref
)

object Response {
    case class Payload( status: String, response: Json )
}