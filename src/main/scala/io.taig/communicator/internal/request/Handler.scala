package io.taig.communicator.internal.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.internal
import io.taig.communicator.internal._

import scala.concurrent.{ExecutionContext => Context}

/**
 * A request that processes the response body without yielding a result
 * 
 * This request handles the response body with an [[io.taig.communicator.internal.response.Handler Handler]] object. A handler
 * does not return a result, but instead does background work with the data (e.g. decoding and then writing to disk).
 * 
 * @param client [[com.squareup.okhttp.OkHttpClient OkHttpClient]]
 * @param request [[com.squareup.okhttp.Request okhttp.Request]]
 * @param handler [[io.taig.communicator.internal.response.Handler]] object to process the response body
 * @param executor Execution context on which the request is executed
 * @see [[io.taig.communicator.internal.request.Parser]]
 * @see [[io.taig.communicator.internal.request.Plain]]
 */
class Handler( val client: OkHttpClient, val request: okhttp.Request, val handler: internal.result.Handler, val executor: Context )
extends
{
	val interceptor = new internal.interceptor.Handler( request, handler )
}
with	Request[response.Handled, internal.event.Send with internal.event.Receive, internal.interceptor.Handler]
with	Receivable

