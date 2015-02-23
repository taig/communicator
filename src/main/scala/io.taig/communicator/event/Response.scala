package io.taig.communicator.event

class	Response
extends	Request
{
	var receive: Option[Progress.Receive => Unit] = None
}