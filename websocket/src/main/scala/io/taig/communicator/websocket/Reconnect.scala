package io.taig.communicator.websocket

import scala.concurrent.duration.FiniteDuration

private case class Reconnect( delay: FiniteDuration ) extends Exception