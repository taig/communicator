package io.taig.communicator.internal.event

trait	Receive
extends	Event
{
	var receive: Option[Progress.Receive => Unit] = None
}