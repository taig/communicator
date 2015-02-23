package io.taig.communicator.event

class Request
{
	var send: Option[Progress.Send => Unit] = None
}