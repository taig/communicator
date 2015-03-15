package io.taig.communicator.internal.interceptor

import com.squareup.okhttp
import io.taig.communicator.internal

class	Plain( val original: okhttp.Request )
extends	Interceptor[internal.response.Plain, internal.event.Send]
with	Write
{
	object	event
	extends	internal.event.Send

	override def wrap( wrapped: okhttp.Response ) = new internal.response.Plain( wrapped )
}