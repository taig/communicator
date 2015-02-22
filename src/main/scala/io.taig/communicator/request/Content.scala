package io.taig.communicator.request

import _root_.io.taig.communicator.body.Receive
import io.taig.communicator.{Parser, Request, Response}
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient

import scala.concurrent.{ExecutionContext => Context}

class	Content[T]( client: OkHttpClient, request: okhttp.Request, parser: Parser[T], executor: Context )
extends	Payload( client, request, executor )
with	Request[Response.Parsable[T]]
{
	override def response( wrapped: okhttp.Response ) =
	{
		receive = new Receive( wrapped.body(), listener.receive )
		new Response.Parsable[T]( receive, wrapped, parser )
	}
}