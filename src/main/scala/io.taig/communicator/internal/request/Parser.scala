package io.taig.communicator.internal.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.internal
import io.taig.communicator.internal._

import scala.concurrent.{ExecutionContext => Context}

/**
 * A request that processes the response body, yielding a result object of type <code>T</code>
 * 
 * This request handles the response body with a [[io.taig.communicator.internal.response.Parser Parser]] object. A parser
 * transforms the incoming data stream to an object of type <code>T</code>.
 * 
 * @param client [[com.squareup.okhttp.OkHttpClient OkHttpClient]]
 * @param request [[com.squareup.okhttp.Request OkHttp okhttp.Request]]
 * @param parser [[io.taig.communicator.internal.response.Parser]] object to process the response body
 * @param executor Execution context on which the request is executed
 * @tparam T Expected result type of the processed response body
 * @see [[io.taig.communicator.internal.request.Handler]]
 * @see [[io.taig.communicator.internal.request.Plain]]
 */
class Parser[T]( val client: OkHttpClient, val request: okhttp.Request, val parser: internal.result.Parser[T], val executor: Context )
extends
{
	val interceptor = new internal.interceptor.Parser( request, parser )
}
with	Request[response.Parsed[T], internal.event.Send with internal.event.Receive, internal.interceptor.Parser[T]]
with	Receivable