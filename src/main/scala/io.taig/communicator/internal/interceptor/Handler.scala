package io.taig.communicator.internal.interceptor

import com.squareup.okhttp
import io.taig.communicator.internal

class	Handler( val original: okhttp.Request, handler: internal.result.Handler )
extends	Interceptor[internal.response.Handled, internal.event.Send with internal.event.Receive]
with	Write
with	Read
{
	object	event
	extends	internal.event.Send
	with	internal.event.Receive

	override def wrap( wrapped: okhttp.Response ) = response match
	{
		case Some( body ) => new internal.response.Handled( wrapped, body, handler )
		case None => sys.error( "Response body has not been created yet" )
	}
}