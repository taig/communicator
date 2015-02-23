package io.taig.communicator

import com.squareup.okhttp
import com.squareup.okhttp.internal.Util.closeQuietly
import io.taig.communicator
import io.taig.communicator.result.{Handler, Parser}

class Response private[communicator]( wrapped: okhttp.Response )
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

	class	Handleable private[communicator]( wrapped: okhttp.Response, body: communicator.body.Response, handler: Handler )
	extends	Response( wrapped )
	{
		handler.handle( this, body.source().inputStream() )
	}

	object Handleable
	{
		def unapply( response: Handleable ) = Response.unapply( response )
	}

	class	Parsable[T] private[communicator]( wrapped: okhttp.Response, body: communicator.body.Response, parser: Parser[T] )
	extends	Response( wrapped )
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