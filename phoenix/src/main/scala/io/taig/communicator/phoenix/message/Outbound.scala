package io.taig.communicator.phoenix.message

import io.circe.Json
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

sealed trait Outbound {
    def topic: Topic

    def event: Event

    def payload: Json

    def ref: Ref
}

case class Request(
    topic:   Topic,
    event:   Event,
    payload: Json,
    ref:     Ref
) extends Outbound