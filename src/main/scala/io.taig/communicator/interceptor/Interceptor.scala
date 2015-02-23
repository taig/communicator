package io.taig.communicator.interceptor

import io.taig.communicator._
import com.squareup.okhttp
import com.squareup.okhttp.Interceptor.Chain

trait	Interceptor[+R <: Response, +E <: event.Request]
extends	okhttp.Interceptor
with	Cancelable
{
	def original: okhttp.Request

	def event: E

	// TODO improve cancel handling, if necessary at all: validate okhttp cancel & if it sucks check for cancel in
	// TODO beginning of intercept and support concurrency
	override def intercept( chain: Chain ) =
	{
		val request = send( chain.request() )
		println( "Sending request %s on %s%n%s".format( request.url(), chain.connection(), request.headers() ) )
		val response = receive( original, chain.proceed( request ) )
		println( "Received response for %s on %n%s".format( response.request().url(), response.headers() ) )
		response
	}

	protected def send( request: okhttp.Request ): okhttp.Request

	protected def receive( original: okhttp.Request, response: okhttp.Response ): okhttp.Response

	def wrap( wrapped: okhttp.Response ): R
}