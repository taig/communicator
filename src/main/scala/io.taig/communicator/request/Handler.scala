package io.taig.communicator.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator

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
 * @see [[io.taig.communicator.request.Parser]]
 * @see [[io.taig.communicator.request.Plain]]
 */
class Handler( val client: OkHttpClient, val request: okhttp.Request, val handler: communicator.result.Handler, val executor: Context )
extends
{
	val interceptor = new communicator.interceptor.Handler( request, handler )
}
with	Request[communicator.response.Handled, communicator.event.Send with communicator.event.Receive, communicator.interceptor.Handler]
with	Receivable

