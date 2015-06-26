package io.taig.communicator

import com.squareup.okhttp
import com.squareup.okhttp.Interceptor.Chain

import scala.language.reflectiveCalls

class	Interceptor( request: okhttp.Request )
extends	okhttp.Interceptor
{
	private val event = new
	{
		var send: Progress.Send => Unit = null
	
		var receive: Progress.Receive => Unit = null
	}

	private val wrapper = new
	{
		var request: body.Request = null

		var response: body.Response = null
	}

	def onSend( f: Progress.Send => Unit ) =
	{
		event.send = f

		if( wrapper.request != null )
		{
			wrapper.request.event = f
		}
	}

	def onReceive( f: Progress.Receive => Unit ) =
	{
		event.receive = f

		if( wrapper.response != null )
		{
			wrapper.response.event = f
		}
	}

	override def intercept( chain: Chain ) =
	{
		val request = send( chain.request() )
		receive( chain.proceed( request ) )
	}

	protected def send( request: okhttp.Request ): okhttp.Request =
	{
		if( request.body() != null )
		{
			wrapper.request = new body.Request( request.body(), event.send )

			request
				.newBuilder()
				.method( request.method(), wrapper.request )
				.build()
		}
		else
		{
			request
		}
	}

	protected def receive( response: okhttp.Response ) =
	{
		if( response.body() != null )
		{
			val builder = response.newBuilder()

			// Take over OkHttp's "transparent GZIP"
			wrapper.response = if(
				request.header( "Accept-Encoding" ) == null &&
				response.header( "Content-Encoding" ) == "gzip"
			) {
				builder
					.removeHeader( "Content-Encoding" )
					.removeHeader( "Content-Length" )

				new body.Response( response.body(), event.receive, true )
			}
			else
			{
				new body.Response( response.body(), event.receive, false )
			}

			builder.body( wrapper.response ).build()
		}
		else
		{
			response
		}

	}
}