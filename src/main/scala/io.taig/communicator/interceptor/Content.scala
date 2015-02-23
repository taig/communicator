package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._
import io.taig.communicator.result.Parser

class	Content[T]( val original: okhttp.Request, parser: Parser[T] )
extends	Interceptor[Response.Parsable[T], event.Response]
with	Write
with	Read
{
	override val event = new communicator.event.Response

	override def wrap( wrapped: okhttp.Response ): Response.Parsable[T] = response match
	{
		case Some( body ) => new Response.Parsable( wrapped, body, parser )
		case None => sys.error( "Response body has not been created yet" )
	}
}