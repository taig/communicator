package io.taig.communicator.websocket

sealed trait Event[+T]

object Event {
    case class OnNext[T]( value: T ) extends Event[T]
    case class OnError( exception: Throwable ) extends Event[Nothing]
    case object OnComplete extends Event[Nothing]
}