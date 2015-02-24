package io.taig.communicator.event

trait	Receive
extends	Event
{
	var receive: Option[Progress.Receive => Unit] = None
}