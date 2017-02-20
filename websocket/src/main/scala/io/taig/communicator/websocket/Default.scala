package io.taig.communicator.websocket

import scala.concurrent.duration._

object Default {
    val failureReconnect: Option[FiniteDuration] = None

    val completeReconnect: Option[FiniteDuration] = None
}