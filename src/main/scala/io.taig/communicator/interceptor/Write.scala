package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator.{Response, body}

trait	Write
extends	Interceptor[Response, communicator.event.Send]
{
	protected var request: Option[body.Request] = None

	override protected def send( request: okhttp.Request ): okhttp.Request =
	{
		if( request.body() != null )
		{
			this.request = Some(
				new body.Request( request.body(), event.send )
			)
	
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
