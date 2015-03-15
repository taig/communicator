package io.taig.communicator.internal.interceptor

import com.squareup.okhttp
import io.taig.communicator.internal
import io.taig.communicator.internal.response.Parsed

class	Parser[T]( val original: okhttp.Request, parser: internal.result.Parser[T] )
extends	Interceptor[internal.response.Parsed[T], internal.event.Send with internal.event.Receive]
with	Write
with	Read
{
	object	event
	extends	internal.event.Send
	with	internal.event.Receive

	override def wrap( wrapped: okhttp.Response ) = response match
	{
		case Some( body ) => new Parsed( wrapped, body, parser )
		case None => sys.error( "Response body has not been created yet" )
	}
}