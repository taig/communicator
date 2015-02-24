package io.taig.communicator.interceptor

import com.squareup.okhttp
import com.squareup.okhttp.Interceptor.Chain
import io.taig.communicator

trait	Interceptor[+R <: communicator.response.Plain, +E <: communicator.event.Event]
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