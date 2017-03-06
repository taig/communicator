package io.taig.communicator.websocket

import scala.concurrent.duration._

object Default {
    val errorReconnect: Int ⇒ Option[FiniteDuration] = _ ⇒ None

    val completeReconnect: Int ⇒ Option[FiniteDuration] = _ ⇒ None
}