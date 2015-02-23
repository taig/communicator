package io.taig.communicator.interceptor

import com.squareup.okhttp
import com.squareup.okhttp.Request
import io.taig.communicator
import io.taig.communicator.{Response, body}

trait	Read
extends	Write
with	Interceptor[Response, communicator.event.Response]
{
	protected var response: Option[body.Response] = None

	override def cancel() =
	{
		super.cancel()
		response.foreach( _.cancel() )
	}

	override def isCanceled = super.isCanceled && response.exists( _.isCanceled )

	override protected def receive( original: Request, response: okhttp.Response ) =
	{
		// Take over OkHttp's "transparent GZIP"
		if( original.header( "Accept-Encoding" ) == null && response.header( "Content-Encoding" ) == "gzip" )
		{
			this.response = Some( new body.Response( response.body(), event.receive, true ) )

			response
				.newBuilder()
				.removeHeader( "Content-Encoding" )
				.removeHeader( "Content-Length" )
				.body( this.response.get )
				.build()
		}
		else
		{
			this.response = Some( new body.Response( response.body(), event.receive, false ) )

			response
				.newBuilder()
				.body( this.response.get )
				.build()
		}
	}
}
