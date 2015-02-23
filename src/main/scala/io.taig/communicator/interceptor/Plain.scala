package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator._

class	Plain( val original: okhttp.Request )
extends	Interceptor[Response, event.Send]
with	Write
{
	object	event
	extends	communicator.event.Send

	override def wrap( wrapped: okhttp.Response ) = new Response( wrapped )
}