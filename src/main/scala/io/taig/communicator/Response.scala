package io.taig.communicator

import com.squareup.okhttp

class Response private[communicator]( wrapped: okhttp.Response )
{
	def code = wrapped.code()

	def message = wrapped.message()

	def headers = wrapped.headers

	def request = wrapped.request

	def protocol = wrapped.protocol

	def handshake = wrapped.handshake

	def isSuccessful = wrapped.isSuccessful

	def isRedirect = wrapped.isRedirect

	def challenges = wrapped.challenges

	def cacheControl = wrapped.cacheControl

	def cacheResponse = wrapped.cacheResponse()

	def networkResponse = wrapped.networkResponse()

	def priorResponse = wrapped.priorResponse()

	private[communicator] def withPayload[T]( parser: Parser[T] ) =
	{
		new Response.Payload(
			wrapped,
			parser.parse( this, wrapped.body().byteStream() )
		)
	}

	override def toString = wrapped.toString
}

object Response
{
	def unapply( response: Response ) = Some( response.code )

	class	Payload[+T] private[communicator]( wrapped: okhttp.Response, payload: T )
	extends	Response( wrapped )
	{
		def body = payload

		def map[A]( f: T => A ) = new Response.Payload[A]( wrapped, f( payload ) )
	}

	object Payload
	{
		def unapply[T]( response: Response.Payload[T] ) = Some( response.code, response.body )
	}
}