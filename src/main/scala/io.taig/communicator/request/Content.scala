package io.taig.communicator.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.body.Receive
import io.taig.communicator.response.Parser
import io.taig.communicator.{Request, Response}

import scala.concurrent.{ExecutionContext => Context}

/**
 * A request that processes the response body, yielding a result object of type <code>T</code>
 * 
 * This request handles the response body with a [[io.taig.communicator.response.Parser Parser]] object. A parser
 * transforms the incoming data stream to an object of type <code>T</code>.
 * 
 * @param client [[com.squareup.okhttp.OkHttpClient OkHttpClient]]
 * @param request [[com.squareup.okhttp.Request OkHttp okhttp.Request]]
 * @param parser [[io.taig.communicator.response.Parser]] object to process the response body
 * @param executor Execution context on which the request is executed
 * @tparam T Expected result type of the processed response body
 * @see [[io.taig.communicator.request.Payload]]
 * @see [[io.taig.communicator.request.Plain]]
 */
class	Content[T]( client: OkHttpClient, request: okhttp.Request, parser: Parser[T], executor: Context )
extends	Payload( client, request, parser, executor )
with	Request[Response.Parsable[T]]
{
	override def response( wrapped: okhttp.Response ) =
	{
		receive = new Receive( wrapped.body(), listener.receive )
		new Response.Parsable[T]( wrapped, receive, parser )
	}
}