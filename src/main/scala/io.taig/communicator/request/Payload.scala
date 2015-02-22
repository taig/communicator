package io.taig.communicator.request

import _root_.io.taig.communicator.body.Receive
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator._

import scala.concurrent.{ExecutionContext => Context}

class	Payload( client: OkHttpClient, request: okhttp.Request, executor: Context )
extends	Plain( client, request, executor )
with	Request[Response.Payload]
{
	protected var receive: Receive = null

	override protected val listener = new Listener

	override def response( wrapped: okhttp.Response ) =
	{
		receive = new Receive( wrapped.body(), listener.receive )
		new Response.Payload( receive, wrapped )
	}

	override def cancel() =
	{
		super.cancel()

		if( receive != null )
		{
			receive.cancel()
		}
	}

	def onReceive( f: Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		listener.receive = ( progress: Progress.Receive ) => executor.execute( f( progress ) )
		this
	}

	protected class	Listener
	extends			super.Listener
	{
		var receive: Progress.Receive => Unit = null
	}
}

