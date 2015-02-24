package io.taig.communicator.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator

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
 * @see [[io.taig.communicator.request.Handler]]
 * @see [[io.taig.communicator.request.Plain]]
 */
class Parser[T]( val client: OkHttpClient, val request: okhttp.Request, val parser: communicator.result.Parser[T], val executor: Context )
extends
{
	val interceptor = new communicator.interceptor.Parser( request, parser )
}
with	Request[communicator.response.Parsed[T], communicator.event.Send with communicator.event.Receive, communicator.interceptor.Parser[T]]
with	Receivable