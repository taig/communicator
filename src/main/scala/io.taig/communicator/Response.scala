package io.taig.communicator

import _root_.io.taig.communicator.response.{Handler, Parser}
import io.taig.communicator.body.Receive
import com.squareup.okhttp
import com.squareup.okhttp.internal.Util.closeQuietly

class Response( wrapped: okhttp.Response )
{
	initialize()

	protected def initialize() = {}

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

	class	Handleable( wrapped: okhttp.Response, body: Receive, handler: Handler )
	extends	Response( wrapped )
	{
		override protected def initialize() = handler.handle( this, body.source().inputStream() )
	}

	object Handleable
	{
		def unapply( response: Handleable ) = Response.unapply( response )
	}

	class	Parsable[T]( wrapped: okhttp.Response, body: Receive, parser: Parser[T] )
	extends	Handleable( wrapped, body, parser )
	{
		override protected def initialize() =
		{
			// Don't execute the handler, populate payload field instead
		}

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