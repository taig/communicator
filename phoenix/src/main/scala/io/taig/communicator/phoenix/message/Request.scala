package io.taig.communicator.phoenix.message

import io.circe.Json
import io.circe.generic.JsonCodec
import io.taig.communicator.phoenix.{ Event, Ref, Topic }

@JsonCodec
case class Request(
    topic:   Topic,
    event:   Event,
    payload: Json  = Json.Null,
    ref:     Ref   = Ref.unique()
)