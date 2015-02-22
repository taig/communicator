package io.taig.communicator

import io.taig.communicator.body.Receive
import com.squareup.okhttp
import com.squareup.okhttp.internal.Util.closeQuietly

class Response( wrapped: okhttp.Response )
{
	def code = wrapped.code

	def message = wrapped.message

	def headers = wrapped.headers

	def request = wrapped.request

	def protocol = wrapped.protocol

	def handshake = wrapped.handshake

	def isSuccessful = wrapped.isSuccessful

	def isRedirect = wrapped.isRedirect

	def challenges = wrapped.challenges

	def cacheControl = wrapped.cacheControl

	val response = new
	{
		def cache = wrapped.cacheResponse()

		def network = wrapped.networkResponse

		def prior = wrapped.priorResponse()
	}

	override def toString = wrapped.toString
}

object Response
{
	def unapply( response: Response ) = Some( response.code )

	class	Payload( val body: Receive, wrapped: okhttp.Response )
	extends	Response( wrapped )

	object Payload
	{
		def unapply( response: Payload ) = Response.unapply( response )
	}

	class	Parsable[T]( body: Receive, wrapped: okhttp.Response, parser: Parser[T] )
	extends	Payload( body, wrapped )
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

	object Parsable
	{
		def unapply[T]( response: Parsable[T] ) = Some( response.code, response.payload )
	}
}