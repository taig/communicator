package io.taig.communicator.internal.event

trait Event
{
	var send: Option[Progress.Send => Unit] = None
}