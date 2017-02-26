package io.taig.communicator.websocket

import scala.concurrent.duration._

object Default {
    val errorReconnect: Option[FiniteDuration] = None

    val completeReconnect: Option[FiniteDuration] = None
}