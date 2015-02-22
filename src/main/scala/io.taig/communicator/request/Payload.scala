package io.taig.communicator.request

import _root_.io.taig.communicator.body.Receive
import _root_.io.taig.communicator.response.Handler
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator._

import scala.concurrent.{ExecutionContext => Context}

/**
 * A request that processes the response body without yielding a result
 * 
 * This request handles the response body with an [[io.taig.communicator.response.Handler Handler]] object. A handler
 * does not return a result, but instead does background work with the data (e.g. decoding and then writing to disk).
 * 
 * @param client [[com.squareup.okhttp.OkHttpClient OkHttpClient]]
 * @param request [[com.squareup.okhttp.Request okhttp.Request]]
 * @param handler [[io.taig.communicator.response.Handler]] object to process the response body
 * @param executor Execution context on which the request is executed
 * @see [[io.taig.communicator.request.Content]]
 * @see [[io.taig.communicator.request.Plain]]
 */
class	Payload( client: OkHttpClient, request: okhttp.Request, handler: Handler, executor: Context )
extends	Plain( client, request, executor )
with	Request[Response.Handleable]
{
	protected var receive: Receive = null

	override protected val listener = new Listener

	override def response( wrapped: okhttp.Response ) =
	{
		receive = new Receive( wrapped.body(), listener.receive )
		new Response.Handleable( wrapped, receive, handler )
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

