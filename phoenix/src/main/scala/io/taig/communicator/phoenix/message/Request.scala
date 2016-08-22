package io.taig.communicator.phoenix.message

import io.circe.Json
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

case class Request(
    topic:   Topic,
    event:   Event,
    payload: Json,
    ref:     Ref
)