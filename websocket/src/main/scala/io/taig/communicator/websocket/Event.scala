package io.taig.communicator.websocket

sealed trait Event[T]

object Event {
    case class Open[T]( socket: WebSocket[T], payload: Option[T] ) extends Event[T]
    case class Message[T]( payload: T ) extends Event[T]
    case class Pong[T]( payload: Option[T] ) extends Event[T]
    case class Failure[T]( exception: Throwable, payload: Option[T] ) extends Event[T]
    case class Close[T]( code: Int, reason: Option[String] ) extends Event[T]
}