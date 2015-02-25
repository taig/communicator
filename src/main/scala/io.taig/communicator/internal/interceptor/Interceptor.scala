package io.taig.communicator.internal.interceptor

import com.squareup.okhttp
import com.squareup.okhttp.Interceptor.Chain
import io.taig.communicator.internal

trait	Interceptor[+R <: internal.response.Plain, +E <: internal.event.Event]
extends	okhttp.Interceptor
{
	def original: okhttp.Request

	def event: E

	override def intercept( chain: Chain ) =
	{
		val request = send( chain.request() )
		receive( chain.proceed( request ) )
	}

	protected def send( request: okhttp.Request ): okhttp.Request

	protected def receive( response: okhttp.Response ): okhttp.Response

	def wrap( wrapped: okhttp.Response ): R
}