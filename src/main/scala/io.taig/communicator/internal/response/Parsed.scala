package io.taig.communicator.internal.response

import com.squareup.okhttp
import com.squareup.okhttp.internal.Util._
import io.taig.communicator.internal

class	Parsed[T] private[communicator]( wrapped: okhttp.Response, body: internal.body.Response, parser: internal.result.Parser[T] )
extends	Plain( wrapped )
{
	val payload: T =
	{
		try
		{
			parser.parse( this, body.source().inputStream() )
		}
		finally
		{
			closeQuietly( body )
		}
	}
}

object Parsed
{
	def unapply[T]( response: Parsed[T] ) = Some( response.code, response.payload )
}