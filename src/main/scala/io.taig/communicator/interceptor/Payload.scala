package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._
import io.taig.communicator.result.Handler

class	Payload( val original: okhttp.Request, handler: Handler )
extends	Interceptor[Response.Handleable, event.Response]
with	Write
with	Read
{
	override val event = new communicator.event.Response

	override def wrap( wrapped: okhttp.Response ) = response match
	{
		case Some( body ) => new Response.Handleable( wrapped, body, handler )
		case None => sys.error( "Response body has not been created yet" )
	}
}