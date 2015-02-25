package io.taig.communicator.internal.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.internal

import scala.concurrent.{ExecutionContext => Context}

/**
 * A simple request that only handles the response's meta data
 * 
 * This request does not process any payload of the response. It is intended for HTTP calls that do not provide a body
 * (e.g. HEAD) or in cases where potential body data is of no interest (e.g. a ping service)
 * 
 * @param client [[com.squareup.okhttp.OkHttpClient OkHttpClient]]
 * @param request [[com.squareup.okhttp.Request okhttp.Request]]
 * @param executor Execution context on which the request is executed
 * @see [[io.taig.communicator.internal.request.Parser]]
 * @see [[io.taig.communicator.internal.request.Handler]]
 */
class Plain( val client: OkHttpClient, val request: okhttp.Request, val executor: Context )
extends
{
	val interceptor = new internal.interceptor.Plain( request )
}
with	Request[internal.response.Plain, internal.event.Send, internal.interceptor.Plain]