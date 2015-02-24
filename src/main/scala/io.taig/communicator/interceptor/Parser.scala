package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator

class	Parser[T]( val original: okhttp.Request, parser: communicator.result.Parser[T] )
extends	Interceptor[communicator.response.Parsed[T], communicator.event.Send with communicator.event.Receive]
with	Write
with	Read
{
	object	event
	extends	communicator.event.Send
	with	communicator.event.Receive

	override def wrap( wrapped: okhttp.Response ): communicator.response.Parsed[T] = response match
	{
		case Some( body ) => new communicator.response.Parsed( wrapped, body, parser )
		case None => sys.error( "Response body has not been created yet" )
	}
}