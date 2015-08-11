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

	override def toString = wrapped.toString
}

object Response
{
	def unapply[T]( response: Response with Payload[T] ) = Some( response.code, response.body )

	class	Payload[+T] private[communicator]( wrapped: okhttp.Response, val body: T )
	extends	Response( wrapped )
}