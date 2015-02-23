package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._

class	Handler( val original: okhttp.Request, handler: result.Handler )
extends	Interceptor[Response.Handleable, event.Send with event.Receive]
with	Write
with	Read
{
	object	event
	extends	communicator.event.Send
	with	communicator.event.Receive

	override def wrap( wrapped: okhttp.Response ) = response match
	{
		case Some( body ) => new Response.Handleable( wrapped, body, handler )
		case None => sys.error( "Response body has not been created yet" )
	}
}