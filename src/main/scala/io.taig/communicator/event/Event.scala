package io.taig.communicator.event

trait Event
{
	var send: Option[Progress.Send => Unit] = None
}