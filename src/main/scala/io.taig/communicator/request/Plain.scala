package io.taig.communicator.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator
import io.taig.communicator._

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
 * @see [[io.taig.communicator.request.Parser]]
 * @see [[io.taig.communicator.request.Handler]]
 */
class Plain( val client: OkHttpClient, val request: okhttp.Request, val executor: Context )
extends
{
	val interceptor = new communicator.interceptor.Plain( request )
}
with	Request[Response, event.Send, communicator.interceptor.Plain]