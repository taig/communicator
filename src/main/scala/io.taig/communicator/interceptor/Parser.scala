package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._

class	Parser[T]( val original: okhttp.Request, parser: result.Parser[T] )
extends	Interceptor[Response.Parsable[T], event.Send with event.Receive]
with	Write
with	Read
{
	object	event
	extends	communicator.event.Send
	with	communicator.event.Receive

	override def wrap( wrapped: okhttp.Response ): Response.Parsable[T] = response match
	{
		case Some( body ) => new Response.Parsable( wrapped, body, parser )
		case None => sys.error( "Response body has not been created yet" )
	}
}