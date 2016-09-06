package io.taig.communicator.phoenix

import io.circe.Json

trait ChannelWriter {
    def send( event: Event, payload: Json ): Unit
}
