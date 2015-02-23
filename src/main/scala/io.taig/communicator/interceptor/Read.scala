package io.taig.communicator.interceptor

import com.squareup.okhttp
import io.taig.communicator._

trait	Read
extends	Write
with	Interceptor[Response, event.Send with event.Receive]
{
	protected var response: Option[body.Response] = None

	override protected def receive( response: okhttp.Response ) =
	{
		val builder = response.newBuilder()

		// Take over OkHttp's "transparent GZIP"
		if( original.header( "Accept-Encoding" ) == null && response.header( "Content-Encoding" ) == "gzip" )
		{
			this.response = Some( new body.Response( response.body(), event.receive, true ) )

			builder
				.removeHeader( "Content-Encoding" )
				.removeHeader( "Content-Length" )
		}
		else
		{
			this.response = Some( new body.Response( response.body(), event.receive, false ) )
		}

		builder.body( this.response.get ).build()
	}
}
