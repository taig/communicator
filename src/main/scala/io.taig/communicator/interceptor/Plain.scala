package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator

class	Plain( val original: okhttp.Request )
extends	Interceptor[communicator.response.Plain, communicator.event.Send]
with	Write
{
	object	event
	extends	communicator.event.Send

	override def wrap( wrapped: okhttp.Response ) = new communicator.response.Plain( wrapped )
}