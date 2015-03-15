package io.taig.communicator.internal.response

import com.squareup.okhttp

class Plain private[communicator]( wrapped: okhttp.Response )
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

object Plain
{
	def unapply( response: Plain ) = Some( response.code )
}