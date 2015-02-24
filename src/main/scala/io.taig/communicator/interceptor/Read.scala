package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator

trait	Read
extends	Write
with	Interceptor[communicator.response.Plain, communicator.event.Send with communicator.event.Receive]
{
	protected var response: Option[communicator.body.Response] = None

	override protected def receive( response: okhttp.Response ) =
	{
		val builder = response.newBuilder()

		// Take over OkHttp's "transparent GZIP"
		if( original.header( "Accept-Encoding" ) == null && response.header( "Content-Encoding" ) == "gzip" )
		{
			this.response = Some( new communicator.body.Response( response.body(), event.receive, true ) )

			builder
				.removeHeader( "Content-Encoding" )
				.removeHeader( "Content-Length" )
		}
		else
		{
			this.response = Some( new communicator.body.Response( response.body(), event.receive, false ) )
		}

		builder.body( this.response.get ).build()
	}
}
