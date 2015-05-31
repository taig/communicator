package io.taig.communicator.internal.interceptor

import com.squareup.okhttp
import io.taig.communicator.internal

trait	Write
extends	Interceptor[internal.response.Plain, internal.event.Send]
{
	protected var request: Option[internal.body.Request] = None

	override protected def send( request: okhttp.Request ): okhttp.Request =
	{
		if( request.body() != null )
		{
			this.request = Some( new internal.body.Request( request.body(), event.send ) )

			request
				.newBuilder()
				.method( request.method(), this.request.get )
				.build()
		}
		else
		{
			request
		}
	}

	override protected def receive( response: okhttp.Response ) = response
}