package io.taig.communicator.interceptor

import com.squareup.okhttp
import com.squareup.okhttp.Interceptor.Chain
import io.taig.communicator._
import io.taig.communicator.event.Event

trait	Interceptor[+R <: Response, +E <: Event]
extends	okhttp.Interceptor
{
	def original: okhttp.Request

	def event: E

	// TODO improve cancel handling, if necessary at all: validate okhttp cancel & if it sucks check for cancel in
	// TODO beginning of intercept and support concurrency
	override def intercept( chain: Chain ) =
	{
		val request = send( chain.request() )
		receive( chain.proceed( request ) )
	}

	protected def send( request: okhttp.Request ): okhttp.Request

	protected def receive( response: okhttp.Response ): okhttp.Response

	def wrap( wrapped: okhttp.Response ): R
}