package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator

class	Handler( val original: okhttp.Request, handler: communicator.result.Handler )
extends	Interceptor[communicator.response.Handled, communicator.event.Send with communicator.event.Receive]
with	Write
with	Read
{
	object	event
	extends	communicator.event.Send
	with	communicator.event.Receive

	override def wrap( wrapped: okhttp.Response ) = response match
	{
		case Some( body ) => new communicator.response.Handled( wrapped, body, handler )
		case None => sys.error( "Response body has not been created yet" )
	}
}