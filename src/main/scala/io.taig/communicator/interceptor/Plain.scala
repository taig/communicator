package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._

class	Plain( val original: okhttp.Request )
extends	Interceptor[Response, event.Request]
with	Write
{
	override val event = new communicator.event.Request

	override def wrap( wrapped: okhttp.Response ) = new Response( wrapped )
}