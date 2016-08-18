package io.taig.communicator.websocket

import monix.reactive.OverflowStrategy

import scala.concurrent.duration._
import scala.language.postfixOps

object Default {
    val reconnect: Option[FiniteDuration] = Some( 3 seconds )

    val strategy = OverflowStrategy.Unbounded
}